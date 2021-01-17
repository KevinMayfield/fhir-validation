/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.mayfieldis.fhirservice;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.mayfieldis.fhirservice.processor.*;
import uk.mayfieldis.hapifhir.FHIRServerProperties;
import uk.mayfieldis.hapifhir.support.ServerFHIRValidation;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * A simple Camel route that triggers from a timer and calls a bean and prints to system out.
 * <p/>
 * Use <tt>@Component</tt> to make Camel auto detect this route when starting.
 */

@Component
public class CamelRouter extends RouteBuilder {

    static final String FHIR_VALIDATION = "direct:FHIR-Validation";

    @Autowired
    FhirContext ctx;

    @Autowired
    ServerFHIRValidation serverFHIRValidation;

    @Override
    public void configure() throws Exception {

        // https://stackoverflow.com/questions/5706166/apache-camel-http-and-ssl



        HRVCsvToObservation HRVCsvToObservation = new HRVCsvToObservation(this.ctx);
        IHealthCsvToObservation iHealthCsvToObservation = new IHealthCsvToObservation(this.ctx);
        MessageSent messageSent = new MessageSent(ctx);

        FHIRValidation validation = new FHIRValidation(this.ctx, this.serverFHIRValidation);
        FHIRMessageToTransaction
                fhirMessageToTransaction = new FHIRMessageToTransaction(ctx);
        FhirException fhirException = new FhirException();
        FHIRResponse fhirResponse = new FHIRResponse(ctx);
        iHealthConnect iHealthConnect = new iHealthConnect(configureSslForHttp());
        iHealthAction iHealthAction = new iHealthAction(configureSslForHttp());
        Clients clients = new Clients();

        onException(HttpOperationFailedException.class)
                .to("log:BaseError?level=ERROR&showAll=true")
                .handled(true)
                .process(fhirException)
                .process(fhirResponse);

        /*

            REST api AND config

         */

        restConfiguration()
                .component("servlet")
                .dataFormatProperty("prettyPrint", "true");
              // Now using spring cors comnfiguration  .enableCORS(true);

        from("rest:get:ping")
                .transform().constant("pong");

        rest("/")
                .get()
                .route()
                .routeId("Ping")
                .transform().constant("Hello World");

        rest("/clients").description("OAuth2 Clients")
                .get()
                .to("direct:clients");
        from("direct:clients")
                .to("log:CONIFG-CLIENT?level=INFO&showAll=true")
                .process(clients);


        rest("/token").description("Withings OAuth2")
                .post()
                .to("direct:token");

        rest("/hrv").description("HRV Transform")
                .post()
                .to("direct:hrv");

        rest("/ihealth").description("IHealth SPO2Transform")
                .post()
                .to("direct:ihealth");


        rest("/ihealth/user/{userID}/{action}")
            .get()
            .to("direct:ihealthApi");

        rest("/ihealth/token").description("IHealth Token")
                .post()
                .to("direct:ihealthtoken");


        from("direct:ihealthApi")
                .to("log:IHEALTH-PRE?level=INFO&showAll=true")
                .process(iHealthAction)
                .to("log:IHEALTH-PRE?level=INFO&showAll=true");

        from("direct:ihealthtoken")

                .setHeader(Exchange.HTTP_PATH, simple("/OpenApiV2/OAuthv2/userauthorization/"))
                .setHeader(Exchange.HTTP_METHOD, simple("POST"))
                .to("log:IHEALTH-PRE?level=INFO&showAll=true")
                .process(iHealthConnect)
                .to("log:IHEALTH-POST?level=INFO&showAll=true");

        from("direct:token")
                .to("log:WITHINGS-PRE?level=INFO&showAll=true")
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, simple("application/x-www-form-urlencoded"))
                .setHeader(Exchange.HTTP_PATH, simple("/oauth2/token"))
                .to("https://account.withings.com/?bridgeEndpoint=true")
                .to("log:WITHINGS-POST?level=INFO&showAll=true");

        from("direct:hrv")
                .to("log:HRVCSV-PRE?level=INFO&showAll=true")
                .process(HRVCsvToObservation);

        from("direct:ihealth")
                .to("log:IHEALTHCSV-PRE?level=INFO&showAll=true")
                .process(iHealthCsvToObservation);


        /*

        FHIR Server interactions

         */
        from("direct:processMessage")
                .routeId("HAPI-ProcessMessage")
                .to(FHIR_VALIDATION)
                .process(messageSent); // Create success operationOutcome message

        from(FHIR_VALIDATION)
                .routeId("FHIR-Validation")
                .setHeader(Exchange.HTTP_PATH,constant(""))
                .process(validation)
                .process(fhirMessageToTransaction)
                .to("log:FHIR-PRE?level=INFO")
             //  .to("file:OUTTX") // debugging
                .onException(HttpOperationFailedException.class).to("log:ERR-Retry?level=ERROR&showException=true&showBody=false")
                .maximumRedeliveries(2).redeliveryDelay(500).handled(false).end()
                .to(FHIRServerProperties.getFHIRServer() + "?bridgeEndpoint=true")
                .to("log:FHIR-POST?level=INFO");
    }

    private SSLContext configureSslForHttp()
    {
        // dev
        String certPassword = "GzbfAByL";
        String certFile = "idscertificate-dev.p12";

        String keyPassword = "password";
        String keyFile="keystore.jks";
        try {



            InputStream certStoreStream = this.getClass().getClassLoader().getResourceAsStream(certFile);
            InputStream keyStoreStream = this.getClass().getClassLoader().getResourceAsStream(keyFile);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            keyStore.load(certStoreStream, certPassword.toCharArray());

            // Think we need to trust the cert here.....

           // keyStore.load(null, null);
            kmf.init(keyStore, certPassword.toCharArray());
            log.debug("Certificate Loaded");

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(keyStoreStream, keyPassword.toCharArray());

            // init the trust manager factory by read certificates
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // 3. init the SSLContext using kmf and tmf above
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            SSLContext.setDefault(sslContext);
            return sslContext;


        } catch (Exception ex) {
           ex.printStackTrace();
           log.error("SSL "+ ex.getMessage());
        }

        return null;
    }

}
