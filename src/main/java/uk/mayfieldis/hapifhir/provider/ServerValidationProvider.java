package uk.mayfieldis.hapifhir.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Validate;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.ValidationModeEnum;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ServerValidationProvider {

    @Autowired
    FhirContext ctx;

    @Autowired
    private ResourceTestProviderR4 resourceTestProvider;

    private static final Logger log = LoggerFactory.getLogger(ServerValidationProvider.class);


    @Validate
    public MethodOutcome testResource(@ResourceParam IBaseResource resource,
                                      @Validate.Mode ValidationModeEnum theMode,
                                      @Validate.Profile String theProfile) throws Exception {

        log.debug("Validate using: "+theProfile);

        return resourceTestProvider.testResource(resource,theMode,theProfile);
    }

    @Operation(name = "$convert", idempotent = true)
    public IBaseResource convertJson(
            @ResourceParam IBaseResource resource
    ) throws Exception {
        return resource;

    }






}
