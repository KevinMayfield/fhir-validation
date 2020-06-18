package uk.mayfieldis.hapifhir.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.hl7.fhir.r4.hapi.rest.server.ServerCapabilityStatementProvider;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.utilities.cache.NpmPackage;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;

public class IGConformanceProvider extends ServerCapabilityStatementProvider {

    private volatile CapabilityStatement capabilityStatement;

    private Instant lastRefresh;

    NpmPackage serverIgPackage;

    FhirContext ctx;

    IGConformanceHelper igConformanceHelper;

    public IGConformanceProvider(RestfulServer theRestfulServer, NpmPackage _serverIgPackage, FhirContext _ctx) {
        super(theRestfulServer);
        serverIgPackage = _serverIgPackage;
        ctx = _ctx;
        igConformanceHelper = new IGConformanceHelper(ctx,serverIgPackage);
    }

    @Override
    public void setRestfulServer(RestfulServer theRestfulServer) {
        super.setRestfulServer(theRestfulServer);
    }

    @Override
    @Metadata
    public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
        if (capabilityStatement != null) {
            if (lastRefresh != null) {
                java.time.Duration duration = java.time.Duration.between(Instant.now(), lastRefresh);
                // May need to revisit
                if ((duration.getSeconds() * 60) < 2) return capabilityStatement;
            }
        }
        lastRefresh = Instant.now();

        capabilityStatement = super.getServerConformance(theRequest, theRequestDetails);

        return  igConformanceHelper.getCapabilityStatement(capabilityStatement);
    }


}
