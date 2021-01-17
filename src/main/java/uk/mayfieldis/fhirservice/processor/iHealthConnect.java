package uk.mayfieldis.fhirservice.processor;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import uk.mayfieldis.hapifhir.FHIRServerProperties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

public class iHealthConnect implements Processor {

    FhirContext ctx = null;
    SSLContext ssl = null;

    public iHealthConnect(SSLContext _ssl) {
        this.ssl = _ssl;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(iHealthConnect.class);


    @Override
    public void process(Exchange exchange) throws Exception {

        HttpsURLConnection.setDefaultSSLSocketFactory(ssl.getSocketFactory());
        URL url = new URL("https://openapi.ihealthlabs.eu/OpenApiV2/OAuthv2/userauthorization/");

        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("POST");

        con.setReadTimeout(10000);
        con.setConnectTimeout(15000);
        con.setRequestMethod("POST");
        con.setDoInput(true);
        con.setDoOutput(true);

        OutputStream os = con.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        if (exchange.getIn().getHeader(Exchange.HTTP_QUERY) != null) {
            log.debug(exchange.getIn().getHeader(Exchange.HTTP_QUERY).toString());
            writer.write(exchange.getIn().getHeader(Exchange.HTTP_QUERY).toString());
            writer.write("&client_id="+ FHIRServerProperties.getIhealthClientId() +
                    "&client_secret="+FHIRServerProperties.getIhealthClientSecret());
        }
        writer.flush();
        writer.close();
        os.close();
        con.connect();

        System.out.println(con.getResponseCode());
        if (con.getResponseCode() <300) {
            exchange.getIn().setBody(con.getInputStream());
        } else {
            exchange.getIn().setBody(con.getErrorStream());
        }

    }
}
