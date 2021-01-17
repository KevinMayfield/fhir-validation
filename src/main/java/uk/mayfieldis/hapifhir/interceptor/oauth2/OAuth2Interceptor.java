package uk.mayfieldis.hapifhir.interceptor.oauth2;


import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import uk.mayfieldis.hapifhir.FHIRServerProperties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Interceptor which checks that a valid OAuth2 Token has been supplied.
 * Checks the following rules:
 *   1.  A token is supplied in the Authorization Header
 *   2.  The token is a valid OAuth2 Token
 *   3.  The token is for the correct server
 *   4.  The token has not expired
 *
 * Ignored if this request is in the list of excluded URIs (e.g. metadata)
 *
 */
@Interceptor
public class OAuth2Interceptor {

    private final List<String> excludedPaths =  new ArrayList<>();

    Logger log = LoggerFactory.getLogger(OAuth2Interceptor.class);

    private ApplicationContext appCtx;

    private static final Pattern RESOURCE_PATTERN = Pattern.compile("^/(\\w+)[//|\\?]?.*$");

    private RsaVerifier verifier;

    private JSONObject jwksObj;

    public OAuth2Interceptor(ApplicationContext context) throws Exception {

        JSONObject openIdObj = null;

        log.trace("OAuth2 init");
        excludedPaths.add("/metadata");
        appCtx = context;

        if (FHIRServerProperties.getSecurityOAuth2()) {
            log.trace("OAuth2 active");
            HttpClient client = getHttpClient();
            log.trace("OAuth2 openid = "+ FHIRServerProperties.getSecurityOAuth2Config());
            HttpGet request = new HttpGet(FHIRServerProperties.getSecurityOAuth2Config());
            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            request.setHeader(HttpHeaders.ACCEPT, "application/json");

            try {

                HttpResponse response = client.execute(request);

                if (response.getStatusLine().toString().contains("200")) {
                    InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
                    BufferedReader bR = new BufferedReader(reader);
                    String line = "";

                    StringBuilder responseStrBuilder = new StringBuilder();
                    while ((line = bR.readLine()) != null) {
                        responseStrBuilder.append(line);
                    }
                    openIdObj = new JSONObject(responseStrBuilder.toString());
                }
            } catch (UnknownHostException e) {
                log.error("Host not known");
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
            if (openIdObj != null && openIdObj.has("jwks_uri"))  {
                log.debug("Calling jwks endpoint " + openIdObj.getString("jwks_uri"));
                request = new HttpGet(openIdObj.getString("jwks_uri"));
                request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                request.setHeader(HttpHeaders.ACCEPT, "application/json");
                try {

                    HttpResponse response = client.execute(request);

                    if (response.getStatusLine().toString().contains("200")) {
                        InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
                        BufferedReader bR = new BufferedReader(reader);
                        String line = "";

                        StringBuilder responseStrBuilder = new StringBuilder();
                        while ((line = bR.readLine()) != null) {
                            responseStrBuilder.append(line);
                        }
                        log.trace(responseStrBuilder.toString());
                        jwksObj = new JSONObject(responseStrBuilder.toString());
                        // https://auth0.com/blog/navigating-rs256-and-jwks/
                        if (jwksObj.has("keys")) {
                            JSONArray keys = jwksObj.getJSONArray("keys");
                            for (int i = 0; i < keys.length(); i++) {
                                Object object = keys.getJSONObject(i);
                                if (object instanceof JSONObject) {
                                    JSONObject keyObj = (JSONObject) object;
                                    if (keyObj.has("kty") && keyObj.getString("kty").equals("RSA")) {

                                        BigInteger modulus = new BigInteger(1, Base64.decodeBase64(keyObj.getString("n")));
                                        BigInteger exponent = new BigInteger(1, Base64.decodeBase64(keyObj.getString("e")));


                                        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                                        KeyFactory factory = KeyFactory.getInstance("RSA");
                                        PublicKey key = factory.generatePublic(spec);

                                        verifier = new RsaVerifier((RSAPublicKey) key, "SHA256withRSA");
                                        if (verifier == null) throw new InternalErrorException("Unable to process public key");
                                    }
                                }
                            }
                        }
                    }
                } catch (UnknownHostException e) {
                    log.error("Host not known");
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }

        }

    }

    private HttpClient getHttpClient(){
        final HttpClient httpClient = HttpClientBuilder.create().build();
        return httpClient;
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
    public boolean incomingRequestPreProcessed(HttpServletRequest theRequest, HttpServletResponse theResponse) {

        String resourcePath = theRequest.getPathInfo();
        log.trace("Accessing Resource {} Method {}",resourcePath,theRequest.getMethod());

        if (excludedPaths.contains(resourcePath)
                || (FHIRServerProperties.getSecurityOAuth2AllowReadOnly() && theRequest.getMethod().equals( "GET"))){
            log.debug("Accessing unprotected resource {}", resourcePath);
            return true;
        }


        String authorizationHeader = theRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null){
            log.warn("OAuth2 Authentication failure.  No OAuth Token supplied in Authorization Header on Request.");
            throw new AuthenticationException("Unauthorised access to protected resource");
        }
        OAuthToken oAuthToken = OAuthTokenUtil.parseOAuthToken(authorizationHeader, verifier);

        // Check that the OAuth Token has not expired
        if (oAuthToken.isExpired()){
            log.warn("OAuth2 Authentication failure due to expired token");
            throw new AuthenticationException("OAuth2 Authentication Token has expired.");
        }
        // Check that the Scopes on the Token allow access to the specified resource

        String resourceName = extractResourceName(resourcePath);
        if (!allowedAccess(resourceName, theRequest.getMethod(), oAuthToken)){
            log.warn("OAuth2 Authentication failed due to insufficient access rights: ");
            throw new ForbiddenOperationException(String.format("Insufficient Access Rights to access %s.", resourceName));
        }


        log.debug("Authenticated Access to {}",resourcePath);
        return true;
    }
    public String extractResourceName(String resourcePath) {
        if (resourcePath == null || !resourcePath.isEmpty()) {
            return "/";
        }
        Matcher match = RESOURCE_PATTERN.matcher(resourcePath);
        if (!match.matches()){
            log.warn("{} does not match secured pattern", resourcePath);
            return "";
        }
        return match.group(1);
    }

    public boolean allowedAccess(String resourceName, String method, OAuthToken oAuthToken) {
        log.trace(FHIRServerProperties.getSecurityOAuth2RequiredScope());

        if (inList(oAuthToken.getScopes(), FHIRServerProperties.getSecurityOAuth2RequiredScope())) {
            log.debug("Access to {} is unrestricted.", resourceName);
            return true;
        } else {
            if (oAuthToken.getScopes() != null) {
                log.warn("Unable to find scope {} in {}", FHIRServerProperties.getSecurityOAuth2RequiredScope(), oAuthToken.getScopes().toString() );
            } else {
                log.warn("Unable to find scope {}", FHIRServerProperties.getSecurityOAuth2RequiredScope() );
            }
            return false;
        }
    }
    private boolean inList(List<String> scopes,String scope) {
        if (scopes == null || scopes.isEmpty()) return false;
        for(String str: scopes) {

            if(str.trim().contains(scope))
                return true;
        }
        return false;
    }

}
