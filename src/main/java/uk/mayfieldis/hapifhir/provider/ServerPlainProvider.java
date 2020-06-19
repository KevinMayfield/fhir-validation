package uk.mayfieldis.hapifhir.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import org.hl7.fhir.convertors.VersionConvertor_30_40;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerPlainProvider {

    @Autowired
    FhirContext ctx;



    private static final Logger log = LoggerFactory.getLogger(ServerPlainProvider.class);



    @Operation(name = "$convert", idempotent = true)
    public IBaseResource convertJson(
            @ResourceParam IBaseResource resource
    ) throws Exception {
        return resource;

    }

    @Operation(name = "$convertR4", idempotent = true)
    public IBaseResource convertR4(
            @ResourceParam IBaseResource resource
    ) throws Exception {

        VersionConvertor_30_40 convertor = new VersionConvertor_30_40();
        org.hl7.fhir.dstu3.model.Resource resourceR3 = (org.hl7.fhir.dstu3.model.Resource) resource;
        Resource resourceR4 = convertor.convertResource(resourceR3,true);

        return resourceR4;

    }


}
