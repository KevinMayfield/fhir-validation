package uk.mayfieldis.hapifhir.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.apache.camel.*;
import org.apache.http.HttpHeaders;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.mayfieldis.hapifhir.support.OperationOutcomeFactory;
import uk.mayfieldis.hapifhir.support.ProviderResponseLibrary;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;


@Component
public class ServerProcessMessageProvider {

    @Autowired
    FhirContext ctx;

    @Autowired
    CamelContext context;

    private static final Logger log = LoggerFactory.getLogger(ServerProcessMessageProvider.class);

    private Exchange buildBundlePost(Exchange exchange,HttpServletRequest httpRequest,IBaseResource resource ) throws IOException {
        exchange.getIn().setBody(ctx.newJsonParser().encodeResourceToString(resource));

        Enumeration<String> names = httpRequest.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String value = httpRequest.getHeader(name);
            exchange.getIn().setHeader(name, value);
        }

        exchange.getIn().setHeader(Exchange.HTTP_QUERY, "");
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getIn().setHeader(Exchange.HTTP_PATH, "$process-message");
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, httpRequest.getHeader(Exchange.CONTENT_TYPE));
        exchange.getIn().setHeader(HttpHeaders.ACCEPT,"application/fhir+json");

        return exchange;
    }



    @Operation(name = "$process-message", idempotent = true)
    public IBaseResource processMessage(HttpServletRequest httpRequest, @ResourceParam IBaseResource resource
    ) throws Exception {


        if (resource instanceof Bundle) {
            Bundle bundle = (Bundle) resource;
            if (!bundle.getType().equals(Bundle.BundleType.MESSAGE)) {
                OperationOutcomeFactory.convertToException(OperationOutcomeFactory.createOperationOutcome("Not a FHIR Message"));
            }
        } else {
            OperationOutcomeFactory.convertToException(OperationOutcomeFactory.createOperationOutcome("Unexpected resource type"));
        }


        ProducerTemplate template = context.createProducerTemplate();

        log.info(httpRequest.getHeader(Exchange.CONTENT_TYPE));
        try {
            Exchange exchange = template.send("direct:processMessage", ExchangePattern.InOut, new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange = buildBundlePost(exchange, httpRequest,resource);
                    if (exchange.getException() != null) {

                    }
                }
            });
            if (exchange.getException() != null) {
                ProviderResponseLibrary.handleException(ctx, exchange.getException());
            }

             IBaseResource baseresource = null;
            if (exchange.getIn().getBody() instanceof InputStream) {
                baseresource = ctx.newJsonParser().parseResource((InputStream) exchange.getIn().getBody());
            } else  if (exchange.getIn().getBody() instanceof String) {
                baseresource =  ctx.newJsonParser().parseResource((String) exchange.getIn().getBody());
            }
            if (baseresource instanceof OperationOutcome) {
                OperationOutcome outcome = (OperationOutcome) baseresource;
                if (outcome.getIssueFirstRep().hasSeverity()
                && (
                        outcome.getIssueFirstRep().getSeverity().equals(OperationOutcome.IssueSeverity.FATAL)
                    ||   outcome.getIssueFirstRep().getSeverity().equals(OperationOutcome.IssueSeverity.ERROR))) {
                    OperationOutcomeFactory.convertToException(outcome);
                }
                if (outcome.getIssueFirstRep().hasCode() && !outcome.getIssueFirstRep().getCode().equals(OperationOutcome.IssueType.INFORMATIONAL)) {
                    OperationOutcomeFactory.convertToException(outcome);
                }
            }
            if (baseresource!=null) return baseresource;

      } catch (BaseServerResponseException srv) {
        // HAPI Exceptions pass through
            throw srv;
        } catch(Exception ex) {
            ProviderResponseLibrary.handleException(ctx, ex);
        }
        return null;
    }




}
