package uk.mayfieldis.hapifhir.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import org.hl7.fhir.convertors.VersionConvertor_30_40;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.dstu3.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ServerConvertProvider {

    @Autowired
    @Qualifier("r3ctx")
    FhirContext r3ctx;

    @Autowired
    @Qualifier("r4ctx")
    FhirContext r4ctx;


    private static final Logger log = LoggerFactory.getLogger(ServerConvertProvider.class);



    @Operation(name = "$convert", idempotent = true)
    public IBaseResource convertJson(
            @ResourceParam IBaseResource resource
    ) throws Exception {
        return resource;

    }

    @Operation(name = "$convertR4", idempotent = true)
    public org.hl7.fhir.r4.model.Resource convertR4(
            @ResourceParam IBaseResource resource
    ) throws Exception {

        VersionConvertor_30_40 convertor = new VersionConvertor_30_40();

        org.hl7.fhir.r4.model.Resource resourceR4 = convertor.convertResource((Resource) resource,true);
        log.info(r4ctx.newJsonParser().encodeResourceToString(resourceR4));
        return resourceR4;

    }


}
