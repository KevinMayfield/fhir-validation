package uk.mayfieldis.hapifhir.support;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.http.conn.HttpHostConnectException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class ProviderResponseLibrary {
    private static final Logger log = LoggerFactory.getLogger(ProviderResponseLibrary.class);



    private ProviderResponseLibrary() {
    }


    public static void handleException(Exception ex) throws Exception {

     /*   if (ex instanceof HttpO) {
            HttpOperationFailedException httpex = (HttpOperationFailedException) ex;
            if (httpex.getResponseBody() != null) {
                throw new UnprocessableEntityException(httpex.getResponseBody());
            } else {
                throw new UnprocessableEntityException(httpex.getMessage());
            }
        } else */
            if (ex instanceof HttpHostConnectException) {
            throw new InternalErrorException("Unable to connect to Server");
        } else {
            throw new InternalErrorException(ex.getMessage());
        }

    }

    public static MethodOutcome handleException(MethodOutcome method, Exception ex) {
        if (ex instanceof OperationOutcomeException) {

            OperationOutcomeException outcomeException = (OperationOutcomeException) ex;
            if (outcomeException.getOutcome().hasIssue()) {
                log.error(outcomeException.getOutcome().getIssueFirstRep().getDiagnostics());
            }
            method.setOperationOutcome(outcomeException.getOutcome());
            method.setCreated(false);
        } else {
            log.error(ex.getMessage());
            if (ex.getStackTrace().length >0) {
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = sw.toString();
                log.error(exceptionAsString);

            }
            if (ex.getCause() != null) {
                log.error("Cause {}",ex.getCause());
            }
            method.setCreated(false);
            method.setOperationOutcome(OperationOutcomeFactory.createOperationOutcome(ex.getMessage()));
        }

        createException(null, method.getOperationOutcome());
        // This next line should not be called
        return method;
    }

    public static void createException(FhirContext ctx, IBaseResource resource) {
        if (resource instanceof OperationOutcome)
        {
            OperationOutcome operationOutcome = (OperationOutcome) resource;
            if (ctx != null) {
                String json = ctx.newJsonParser().encodeResourceToString(operationOutcome);
                log.info("Sever Returned: {}", json);
            }

            OperationOutcomeFactory.convertToException(operationOutcome);
        } else {
            throw new InternalErrorException("Server Error");
        }
    }

    public static IBaseResource processMessageBody(FhirContext ctx, IBaseResource resource, Object message ) {
        InputStream inputStream = null;

        if (message instanceof InputStream) {
            log.trace("RESPONSE InputStream");
            inputStream = (InputStream) message;
            Reader reader = new InputStreamReader(inputStream);
            try {
                resource = ctx.newXmlParser().parseResource(reader);
            } catch (Exception ex) {
                resource = ctx.newJsonParser().parseResource(reader);
            }
        } else
        if (message instanceof String) {
            log.trace("RESPONSE String = {}", message);
            try {
                resource = ctx.newXmlParser().parseResource((String) message);
            } catch (Exception ex) {
                resource = ctx.newJsonParser().parseResource((String) message);
            }
            log.trace("RETURNED String Resource {}",resource.getClass().getSimpleName());
        } else {
            log.info("MESSAGE TYPE {}", message.getClass());
        }
        return resource;
    }

    public static MethodOutcome setMethodOutcome (IBaseResource resource, MethodOutcome method) {
        if (resource instanceof OperationOutcome) {
            OperationOutcome opOutcome = (OperationOutcome) resource;
            method.setOperationOutcome(opOutcome);
            method.setCreated(false);
        } else {
            method.setCreated(true);
            OperationOutcome opOutcome = new OperationOutcome();
            method.setOperationOutcome(opOutcome);
            method.setId(resource.getIdElement());
            method.setResource(resource);
        }
        return method;
    }
}
