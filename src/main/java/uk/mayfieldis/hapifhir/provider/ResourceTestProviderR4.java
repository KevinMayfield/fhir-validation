package uk.mayfieldis.hapifhir.provider;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Validate;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.ValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.mayfieldis.hapifhir.FHIRServerProperties;
import uk.mayfieldis.hapifhir.support.ProviderResponseLibrary;
import uk.mayfieldis.hapifhir.support.ServerFHIRValidation;


@Component
public class ResourceTestProviderR4 {


    @Autowired()
	@Qualifier("r4ctx")
    FhirContext ctx;

	@Autowired
    ServerFHIRValidation val;

    private static final Logger log = LoggerFactory.getLogger(ResourceTestProviderR4.class);


    public MethodOutcome testResource(@ResourceParam IBaseResource resourceToValidate,
                                      @Validate.Mode ValidationModeEnum theMode,
                                      @Validate.Profile String theProfile) throws Exception {

        MethodOutcome retVal = new MethodOutcome();
    	if(!FHIRServerProperties.getValidationFlag())
    	{
    		retVal.setOperationOutcome(null);
    		return retVal;
    	}

		if (resourceToValidate == null) {
			Exception e = new InternalErrorException("Failed conversion to FHIR Resource. (Check resource type matches validation endpoint)");
			ProviderResponseLibrary.handleException(retVal,e);
			return retVal;
		}
		OperationOutcome outcome = validateResource(resourceToValidate, theMode, theProfile);

		retVal.setOperationOutcome(outcome);
		return retVal;

	}



	public OperationOutcome validateResource(IBaseResource resource, ValidationModeEnum theMode,
                                             String theProfile) throws Exception {

		ValidationOptions options = new ValidationOptions();
        if (theProfile != null) options.addProfile(theProfile);

		ValidationResult results = val.validateWithResult(resource,options);

		return (OperationOutcome) results.toOperationOutcome();

	}

}
