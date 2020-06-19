package uk.mayfieldis.hapifhir.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.ValidationModeEnum;
import ca.uhn.fhir.rest.param.StringParam;
import org.hl7.fhir.convertors.VersionConvertor_30_40;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;


@Component
public class ServerValidationProvider {

    @Autowired
    FhirContext ctx;

    @Autowired
    private ResourceTestProviderR4 resourceTestProvider;

    private static final Logger log = LoggerFactory.getLogger(ServerValidationProvider.class);

    @Validate
    public MethodOutcome validate(HttpServletRequest theServletRequest, @ResourceParam IBaseResource resource,
                                  @Validate.Mode ValidationModeEnum theMode,
                                  @Validate.Profile String theProfile
                        ) throws Exception {

        if (theServletRequest.getQueryString() != null && theProfile == null) {
            Map<String, String> query_pairs = new LinkedHashMap<String, String>();
            String query = theServletRequest.getQueryString();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
            theProfile = query_pairs.get("profile");
        };
        log.info("Validate using: "+theProfile);

        return resourceTestProvider.testResource(resource,theMode,theProfile);
    }

}
