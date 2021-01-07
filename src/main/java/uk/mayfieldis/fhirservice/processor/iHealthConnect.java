package uk.mayfieldis.fhirservice.processor;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
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

        con.setDoOutput(true);

        con.connect();

        exchange.getIn().setBody(con.getOutputStream());

    }
}
