package uk.mayfieldis.fhirservice.processor;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

public class iHealthAction implements Processor {

    FhirContext ctx = null;
    SSLContext ssl = null;

    public iHealthAction(SSLContext _ssl) {
        this.ssl = _ssl;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(iHealthAction.class);


    @Override
    public void process(Exchange exchange) throws Exception {

        HttpsURLConnection.setDefaultSSLSocketFactory(ssl.getSocketFactory());
        URL url = new URL("https://openapi.ihealthlabs.eu/openapiv2/user/"
                +exchange.getIn().getHeader("userId")
                +"/"
                +exchange.getIn().getHeader("action")
                +"/?"
                +exchange.getIn().getHeader(Exchange.HTTP_QUERY).toString());

        log.info(url.getHost());
        log.info(url.getPath());
        log.info(url.getQuery());
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setReadTimeout(10000);
        con.setConnectTimeout(15000);
        con.setDoInput(true);
        con.setDoOutput(true);

        con.connect();

        log.debug(con.getHeaderFields().toString()); //response headers
        log.debug(String.valueOf(con.getResponseCode()));//response http code

        if (con.getResponseCode() <300) {
            exchange.getIn().setBody(con.getInputStream());
        } else {
            exchange.getIn().setBody(con.getErrorStream());
        }

    }
}
