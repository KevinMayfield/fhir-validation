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
package uk.nhsd.apim.fhirvalidator;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLSocketFactory;

/**
 * A simple Camel route that triggers from a timer and calls a bean and prints to system out.
 * <p/>
 * Use <tt>@Component</tt> to make Camel auto detect this route when starting.
 */

@Component
public class CamelRouter extends RouteBuilder {


    @Override
    public void configure() throws Exception {

        /*
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource("keystore.jks");
        ksp.setPassword("password");

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyStore(ksp);
        kmp.setKeyPassword("keyPassword");

        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);

        HttpComponent httpComponent = getContext().getComponent("https", HttpComponent.class);
        httpComponent.setSslContextParameters(scp);
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

        from("direct:token")
                .to("log:PRE1?level=INFO&showAll=true")
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, simple("application/x-www-form-urlencoded"))
                .setHeader(Exchange.HTTP_PATH, simple("/oauth2/token"))
                .to("log:PRE2?level=INFO&showAll=true")
                .to("https://account.withings.com/?bridgeEndpoint=true")
                .to("log:POST?level=INFO&showAll=true");

    }

}
