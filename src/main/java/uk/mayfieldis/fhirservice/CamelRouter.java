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
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.mayfieldis.fhirservice.processor.*;
import uk.mayfieldis.hapifhir.FHIRServerProperties;
import uk.mayfieldis.hapifhir.support.ServerFHIRValidation;

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

        HRVCsvToObservation HRVCsvToObservation = new HRVCsvToObservation(this.ctx);
        IHealthCsvToObservation iHealthCsvToObservation = new IHealthCsvToObservation(this.ctx);
        MessageSent messageSent = new MessageSent(ctx);

        FHIRValidation validation = new FHIRValidation(this.ctx, this.serverFHIRValidation);
        FHIRMessageToTransaction
                fhirMessageToTransaction = new FHIRMessageToTransaction(ctx);
        FhirException fhirException = new FhirException();
        FHIRResponse fhirResponse = new FHIRResponse(ctx);

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
                .dataFormatProperty("prettyPrint", "true")
                .enableCORS(true);

        from("rest:get:hello")
                .transform().constant("Bye World");

        rest("/")
                .get()
                .route()
                .routeId("Ping")
                .transform().constant("Hello World");

        rest("/token").description("Withings OAuth2")
                .post()
                .to("direct:token");

        rest("/hrv").description("HRV Transform")
                .post()
                .to("direct:hrv");

        rest("/ihealth").description("IHealth SPO2Transform")
                .post()
                .to("direct:ihealth");

        rest("/ihealth/token").description("IHealth Token")
                .post()
                .to("direct:ihealthtoken");

        from("direct:ihealthtoken")
                .to("log:PRE1?level=INFO&showAll=true")
              //  .removeHeaders("*")
                .setHeader(Exchange.HTTP_PATH, simple("/OpenApiV2/OAuthv2/userauthorization/"))
                .setHeader(Exchange.HTTP_METHOD, simple("POST"))
                .to("log:PRE2?level=INFO&showAll=true")
                .to("https://openapi.ihealthlabs.eu/?bridgeEndpoint=true")
                .to("log:POST?level=INFO&showAll=true");

        from("direct:token")
                .to("log:PRE1?level=INFO&showAll=true")
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, simple("application/x-www-form-urlencoded"))
                .setHeader(Exchange.HTTP_PATH, simple("/oauth2/token"))
                .to("log:PRE2?level=INFO&showAll=true")
                .to("https://account.withings.com/?bridgeEndpoint=true")
                .to("log:POST?level=INFO&showAll=true");

        from("direct:hrv")
                .to("log:HRV?level=INFO&showAll=true")
                .process(HRVCsvToObservation);

        from("direct:ihealth")
                .to("log:iHealth?level=INFO&showAll=true")
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
                .to("log:TX-FHIR-Server?level=INFO")
             //   .to("file:OUTTX") // debugging
                .onException(HttpOperationFailedException.class).to("log:ERR-Retry?level=ERROR&showException=true&showBody=false")
                .maximumRedeliveries(2).redeliveryDelay(500).handled(false).end()
                .to(FHIRServerProperties.getFHIRServer());
    }

}
