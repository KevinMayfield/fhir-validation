package uk.mayfieldis.hapifhir.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.utilities.cache.NpmPackage;
import uk.mayfieldis.hapifhir.FHIRServerProperties;

import java.util.concurrent.atomic.AtomicReference;

public class IGConformanceHelper {

    private volatile CapabilityStatement capabilityStatement;

    NpmPackage serverIgPackage;

    FhirContext ctx;

    public IGConformanceHelper(FhirContext ctx, NpmPackage serverIgPackage) {
        this.serverIgPackage = serverIgPackage;
        this.ctx = ctx;
    }

    public CapabilityStatement getCapabilityStatement(CapabilityStatement capabilityStatement) {
        this.capabilityStatement = capabilityStatement;

        if (FHIRServerProperties.getSecurityOAuth2()) {
            for (CapabilityStatement.CapabilityStatementRestComponent nextRest : capabilityStatement.getRest()) {
                nextRest.getSecurity()
                        .addService().addCoding()
                        .setSystem("http://hl7.org/fhir/restful-security-service")
                        .setCode("OAuth")
                        .setDisplay("OAuth2 Bearer Token");

                if (FHIRServerProperties.getSecurityOAuth2Server() != null && !FHIRServerProperties.getSecurityOAuth2Server().isEmpty()) {
                    Extension securityExtension = nextRest.getSecurity().addExtension()
                            .setUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");

                    securityExtension.addExtension()
                            .setUrl("token")
                            .setValue(new UriType(FHIRServerProperties.getSecurityOAuth2Server()+"/token"));
                    securityExtension.addExtension()
                            .setUrl("authorize")
                            .setValue(new UriType(FHIRServerProperties.getSecurityOAuth2Server()+"/authorize"));
                    securityExtension.addExtension()
                            .setUrl("register")
                            .setValue(new UriType(FHIRServerProperties.getSecurityOAuth2Server()+"/register"));
                }
            }
        }

        if (capabilityStatement.hasImplementation() && !FHIRServerProperties.getServerIgUrl().isEmpty()) {
            capabilityStatement.getImplementation()
                    .setUrl(FHIRServerProperties.getServerIgUrl().replace("/package.tgz",""))
                    .setDescription(FHIRServerProperties.getServerIgDescription());
        }

        if (capabilityStatement.hasSoftware()) {
            capabilityStatement.getSoftware().setName(FHIRServerProperties.getSoftwareName());
        }
        if (serverIgPackage != null) {
            getIGDefinition();
        }
        capabilityStatement.setPublisher(FHIRServerProperties.getPublisher());

        return capabilityStatement;
    }

    private void getIGDefinition() {
        try {
            for (String uri : serverIgPackage.listResources("CapabilityStatement")) {
                CapabilityStatement igcapabilityStatement = (CapabilityStatement) ctx.newJsonParser().parseResource(serverIgPackage.load("package", uri));
                for (CapabilityStatement.CapabilityStatementRestComponent igcomponent : igcapabilityStatement.getRest()) {
                    processResourceComponent(igcomponent);
                    processOperationComponent(igcomponent);
                }
                for (CapabilityStatement.CapabilityStatementMessagingComponent messagingComponent : igcapabilityStatement.getMessaging()) {
                    for (CapabilityStatement.CapabilityStatementMessagingSupportedMessageComponent message :messagingComponent.getSupportedMessage()) {
                        if (capabilityStatement.getMessaging().size() == 0) {
                            capabilityStatement.addMessaging();
                        }
                        AtomicReference<Boolean> fd = new AtomicReference<>(false);
                        capabilityStatement.getMessagingFirstRep().getSupportedMessage().forEach( existing -> {
                            if (existing.getDefinition().equals(message.getDefinition())) {
                                fd.set(true);
                            }
                        });
                        if (!fd.get() ) {
                            capabilityStatement.getMessagingFirstRep().addSupportedMessage(message);
                        }
                    }
                }

            }
        } catch (Exception ex) {
            throw new InternalErrorException(ex.getMessage());
        }

    }
    private void processResourceComponent(CapabilityStatement.CapabilityStatementRestComponent igcomponent ) {
        for (CapabilityStatement.CapabilityStatementRestResourceComponent igrestResource : igcomponent.getResource()) {
            for (CapabilityStatement.CapabilityStatementRestComponent component : capabilityStatement.getRest()) {
                for (CapabilityStatement.CapabilityStatementRestResourceComponent restResource : component.getResource()) {
                    if (igrestResource.hasProfile() && restResource.getType().equals(igrestResource.getType())) {
                        restResource.setProfile(igrestResource.getProfile());
                    }
                }
            }
        }

    }
    private void processOperationComponent(CapabilityStatement.CapabilityStatementRestComponent igcomponent ) {
        for (CapabilityStatement.CapabilityStatementRestResourceOperationComponent operationComponent : igcomponent.getOperation()) {
            for (CapabilityStatement.CapabilityStatementRestComponent component : capabilityStatement.getRest()) {
                for (CapabilityStatement.CapabilityStatementRestResourceOperationComponent ownOperationCompoent : component.getOperation()) {
                    if (ownOperationCompoent.getName().equals(operationComponent.getName())) {
                        ownOperationCompoent.setDefinition(operationComponent.getDefinition());
                    }
                }
            }
        }
    }
}
