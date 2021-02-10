package uk.mayfieldis.fhirservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import uk.mayfieldis.hapifhir.FHIRServerProperties;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private static final String AUTHORITY_PREFIX = "SCOPE_";
    private static final String CLAIM_ROLES = "roles";

    @Override
    protected void configure(HttpSecurity http) throws Exception {



/*
// www.baeldung.com/spring-security-oauth-resource-server

Also https://www.baeldung.com/spring-security-oauth2-jws-jwk
Note we cant use the introspection endpoint as cognito doesn't support it.

Do we add code to manually veriy key?
https://stackoverflow.com/questions/48356287/is-there-any-java-example-of-verification-of-jwt-for-aws-cognito-api

THis looks reaonable entry point https://dev.to/toojannarong/spring-security-with-jwt-the-easiest-way-2i43

 */
        http.cors();

        if (FHIRServerProperties.getSecurityOAuth2()) {
            http
                    .csrf().disable()
                    .authorizeRequests()
                    .antMatchers("/R4/**").permitAll();


            http
                    .authorizeRequests(authz -> authz
                            .antMatchers(HttpMethod.GET, "/services/**").hasAuthority("SCOPE_patient/*.*")
                            .antMatchers(HttpMethod.POST, "/services").hasAuthority("SCOPE_patient/*.*")
                    )
                    .exceptionHandling().disable()
                    .oauth2ResourceServer(oauth2ResourceServer ->
                            oauth2ResourceServer
                                    //.authenticationEntryPoint().
                                    .jwt(jwt ->
                                    {
                                        // add stuff here
                                    }))
                    .sessionManagement(sessionManagement ->
                            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));


            http
                    //.csrf().disable()
                    .authorizeRequests()
                    .antMatchers("/error").permitAll()
                    .anyRequest().authenticated();

        } else {
            http
                    .authorizeRequests()
                    .antMatchers("/").permitAll().and().csrf().disable();

            http
                    .authorizeRequests()
                    .antMatchers("/error").permitAll()
                    .antMatchers("/jolokia/**").hasRole("ACTUATOR")
                    .antMatchers("/hawtio/**").hasRole("ACTUATOR")
                    .and().httpBasic();
        }


    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader(HttpHeaders.ORIGIN);
        config.addAllowedHeader(HttpHeaders.ACCEPT);
        config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
        config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
        config.addAllowedHeader(HttpHeaders.CACHE_CONTROL);
        config.addAllowedHeader("x-fhir-starter");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Prefer");
        String allAllowedCORSOrigins = FHIRServerProperties.getCorsAllowedOrigin();
        Arrays.stream(allAllowedCORSOrigins.split(",")).forEach(o -> {
            config.addAllowedOrigin(o);
        });
        config.addAllowedOrigin(FHIRServerProperties.getCorsAllowedOrigin());

        config.addExposedHeader("Location");
        config.addExposedHeader("Content-Location");
        config.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        config.setAllowCredentials(FHIRServerProperties.getCorsAllowedCredentials());

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private JwtAuthenticationConverter getJwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(getJwtGrantedAuthoritiesConverter());
        return jwtAuthenticationConverter;
    }

    private JwtGrantedAuthoritiesConverter getJwtGrantedAuthoritiesConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix(AUTHORITY_PREFIX);
        converter.setAuthoritiesClaimName(CLAIM_ROLES);
        return converter;
    }


}
