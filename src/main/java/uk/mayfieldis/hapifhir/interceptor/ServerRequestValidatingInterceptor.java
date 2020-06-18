package uk.mayfieldis.hapifhir.interceptor;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.method.ResourceParameter;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.mayfieldis.hapifhir.support.ServerFHIRValidation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;
import java.util.List;

public class ServerRequestValidatingInterceptor extends ServerBaseValidatingInterceptor<String> {
    public static final String DEFAULT_RESPONSE_HEADER_NAME = "X-FHIR-Request-Validation";
    private static final Logger ourLog = LoggerFactory.getLogger(ca.uhn.fhir.rest.server.interceptor.RequestValidatingInterceptor.class);
    public static final String REQUEST_VALIDATION_RESULT = ca.uhn.fhir.rest.server.interceptor.RequestValidatingInterceptor.class.getName() + "_REQUEST_VALIDATION_RESULT";
    private boolean myAddValidationResultsToResponseOperationOutcome = true;
    private FhirContext ctx;
    private ServerFHIRValidation val;

    List<CapabilityStatement> capabilityStatements = null;

    public ServerRequestValidatingInterceptor(FhirContext _ctx, ServerFHIRValidation val ) {

        this.ctx = _ctx;
        this.val = val;
    }


    ValidationResult doValidate(FhirValidator xxxValidator, String theRequest) {
        EncodingEnum encoding = EncodingEnum.detectEncodingNoDefault(theRequest);
        IBaseResource resource = encoding.newParser(ctx).parseResource(theRequest);
        return val.validateWithResult(resource);
    }



    @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
    public boolean incomingRequestPostProcessed(RequestDetails theRequestDetails, HttpServletRequest theRequest, HttpServletResponse theResponse) throws AuthenticationException {
        EncodingEnum encoding = RestfulServerUtils.determineRequestEncodingNoDefault(theRequestDetails);
        if (theRequestDetails.getOperation() != null) {
            if (theRequestDetails.getOperation().equals("$convert")) return true;
            if (theRequestDetails.getOperation().equals("$validate")) return true;
        }

        if (encoding == null) {
            ourLog.trace("Incoming request does not appear to be FHIR, not going to validate");
            return true;
        } else {
            Charset charset = ResourceParameter.determineRequestCharset(theRequestDetails);
            String requestText = new String(theRequestDetails.loadRequestContents(), charset);
            if (StringUtils.isBlank(requestText)) {
                ourLog.trace("Incoming request does not have a body");
                return true;
            } else {
                ValidationResult validationResult = this.validate(requestText, theRequestDetails);
                theRequestDetails.getUserData().put(REQUEST_VALIDATION_RESULT, validationResult);
                return true;
            }
        }
    }

    public boolean isAddValidationResultsToResponseOperationOutcome() {
        return this.myAddValidationResultsToResponseOperationOutcome;
    }

    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
    public boolean outgoingResponse(RequestDetails theRequestDetails, IBaseResource theResponseObject) {
        if (this.myAddValidationResultsToResponseOperationOutcome && theResponseObject instanceof IBaseOperationOutcome) {
            IBaseOperationOutcome oo = (IBaseOperationOutcome)theResponseObject;
            if (theRequestDetails != null) {
                ValidationResult validationResult = (ValidationResult)theRequestDetails.getUserData().get(REQUEST_VALIDATION_RESULT);
                if (validationResult != null) {
                    validationResult.populateOperationOutcome(oo);
                }
            }
        }

        return true;
    }

    String provideDefaultResponseHeaderName() {
        return "X-FHIR-Request-Validation";
    }

    public void setAddValidationResultsToResponseOperationOutcome(boolean theAddValidationResultsToResponseOperationOutcome) {
        this.myAddValidationResultsToResponseOperationOutcome = theAddValidationResultsToResponseOperationOutcome;
    }

    public void setResponseHeaderName(String theResponseHeaderName) {
        super.setResponseHeaderName(theResponseHeaderName);
    }
}
