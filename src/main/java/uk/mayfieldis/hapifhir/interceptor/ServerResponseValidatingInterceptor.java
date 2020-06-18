package uk.mayfieldis.hapifhir.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.cache.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.mayfieldis.hapifhir.support.ServerFHIRValidation;

import java.util.HashSet;
import java.util.Set;

public class ServerResponseValidatingInterceptor extends ServerBaseValidatingInterceptor<IBaseResource> {
    public static final String DEFAULT_RESPONSE_HEADER_NAME = "X-FHIR-Response-Validation";
    private static final Logger ourLog = LoggerFactory.getLogger(ca.uhn.fhir.rest.server.interceptor.ResponseValidatingInterceptor.class);

    private Set<RestOperationTypeEnum> myExcludeOperationTypes;
    private ServerFHIRValidation val;

    public ServerResponseValidatingInterceptor(NpmPackage _npm, ServerFHIRValidation val ) {
        this.setNpm(_npm);
        this.val = val;
    }

    public void addExcludeOperationType(RestOperationTypeEnum theOperationType) {
        Validate.notNull(theOperationType, "theOperationType must not be null", new Object[0]);
        if (this.myExcludeOperationTypes == null) {
            this.myExcludeOperationTypes = new HashSet();
        }

        this.myExcludeOperationTypes.add(theOperationType);
    }


    ValidationResult doValidate(FhirValidator theValidator, IBaseResource theRequest) {
        return val.validateWithResult(theRequest);
    }

    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
    public boolean outgoingResponse(RequestDetails theRequestDetails, IBaseResource theResponseObject) {
        RestOperationTypeEnum operationType = theRequestDetails.getRestOperationType();
        if (operationType != null && this.myExcludeOperationTypes != null && this.myExcludeOperationTypes.contains(operationType)) {
            ourLog.trace("Operation type {} is excluded from validation", operationType);
            return true;
        } else {
            this.validate(theResponseObject, theRequestDetails);
            return true;
        }
    }

    String provideDefaultResponseHeaderName() {
        return "X-FHIR-Response-Validation";
    }

    public void setResponseHeaderName(String theResponseHeaderName) {
        super.setResponseHeaderName(theResponseHeaderName);
    }
}