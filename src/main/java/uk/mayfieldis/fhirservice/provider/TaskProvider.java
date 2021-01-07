package uk.mayfieldis.fhirservice.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TaskProvider implements IResourceProvider {


    /*

    Notes: added a group search parameter based on this https://www.smilecdr.com/our-blog/custom-search-parameters-in-hapi-fhir

    {
	"resourceType": "SearchParameter",
	"title": "Group Identifier",
	"base": [ "MedicationRequest" ],
	"status": "active",
	"code": "group",
	"type": "token",
	"expression": "MedicationRequest.groupIdentifier",
	"xpathUsage": "normal"
}

     */
    @Autowired
    FhirContext ctx;

    @Autowired
    IGenericClient client;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Task.class;
    }
    private static final Logger log = LoggerFactory.getLogger(TaskProvider.class);

    @Create
    public MethodOutcome create(@ResourceParam Task task) {
        return client.create()
                .resource(task)
                .execute();

    }

    @Operation(name = "detail", idempotent = true, bundleType = BundleTypeEnum.COLLECTION)
    public Bundle getDetail(  @OperationParam(name=Task.SP_IDENTIFIER) TokenParam group,
                                    @OperationParam(name=Task.SP_PERFORMER) TokenParam performer
                                    ) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        if (group != null) {
            log.info(group.getValue());
            Bundle results = client
                    .search()
                    .forResource(MedicationRequest.class)
                    .where(new TokenClientParam("group").exactly().code(group.getValue()))
                    .include(MedicationRequest.INCLUDE_ALL.asRecursive())
                    .returnBundle(Bundle.class)
                    .execute();
            if (!results.isEmpty()) {
                bundle.addEntry(new Bundle.BundleEntryComponent().setResource(results));
            }
        }

        if (performer != null) {
            Bundle search = client
                    .search()
                    .forResource(MedicationRequest.class)
                    .where(MedicationRequest.INTENDED_DISPENSER.hasChainedProperty(Organization.IDENTIFIER.exactly().code(performer.getValue())))
                    .returnBundle(Bundle.class)
                    .execute();
            if (!search.isEmpty()) {
                for (Bundle.BundleEntryComponent component : search.getEntry()){
                    Bundle results = client
                            .search()
                            .forResource(MedicationRequest.class)
                            .where(MedicationRequest.RES_ID.exactly().code(component.getResource().getId()))
                            .include(MedicationRequest.INCLUDE_ALL.asRecursive())
                            .returnBundle(Bundle.class)
                            .execute();
                    if (!results.isEmpty()) {
                        bundle.addEntry(new Bundle.BundleEntryComponent().setResource(results));
                    }
                }
            }
        }

        return bundle;
    }

    @Operation(name = "tracker", idempotent = true, bundleType = BundleTypeEnum.COLLECTION)
    public Bundle getAvailable(  @OperationParam(name=Task.SP_IDENTIFIER) TokenParam group,
                                    @OperationParam(name=Task.SP_PERFORMER) TokenParam performer
    ) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        if (group != null) {
            log.info(group.getValue());
            Bundle results = client
                    .search()
                    .forResource(MedicationRequest.class)
                    .where(new TokenClientParam("group").exactly().code(group.getValue()))
                    .returnBundle(Bundle.class)
                    .execute();
            if (!results.isEmpty()) {
                bundle.addEntry(new Bundle.BundleEntryComponent().setResource(results));
            }
        }

        if (performer != null) {
            Bundle search = client
                    .search()
                    .forResource(MedicationRequest.class)
                    .where(MedicationRequest.INTENDED_DISPENSER.hasChainedProperty(Organization.IDENTIFIER.exactly().code(performer.getValue())))
                    .returnBundle(Bundle.class)
                    .execute();
            if (!search.isEmpty()) {
                for (Bundle.BundleEntryComponent component : search.getEntry()){
                    Bundle results = client
                            .search()
                            .forResource(MedicationRequest.class)
                            .where(MedicationRequest.RES_ID.exactly().code(component.getResource().getId()))
                            .returnBundle(Bundle.class)
                            .execute();
                    if (!results.isEmpty()) {
                        bundle.addEntry(new Bundle.BundleEntryComponent().setResource(results));
                    }
                }
            }
        }

        return bundle;
    }

    @Operation(name = "release", idempotent = true, bundleType = BundleTypeEnum.COLLECTION)
    public Bundle compositionDocumentOperation(
            @ResourceParam Parameters parameters
    ) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        log.info(String.valueOf(parameters.getParameter().size()));
        for (Parameters.ParametersParameterComponent parameterComponent : parameters.getParameter()) {
            if (parameterComponent.hasName() && parameterComponent.getName().equals("owner")) {
                if (parameterComponent.getValue() instanceof Identifier) {
                    Identifier identifier = (Identifier) parameterComponent.getValue();
                    log.info(identifier.getValue() + " - " + identifier.getSystem());
                    Bundle search = client
                            .search()
                            .forResource(MedicationRequest.class)
                            .where(MedicationRequest.INTENDED_DISPENSER.hasChainedProperty(Organization.IDENTIFIER.exactly().code(identifier.getValue())))
                            .returnBundle(Bundle.class)
                            .execute();
                    if (!search.isEmpty()) {



                        for (Bundle.BundleEntryComponent component : search.getEntry()){
                            MedicationRequest medicationRequest = (MedicationRequest) component.getResource();
                            Bundle resultBundle = client.search()
                                    .forResource(Task.class)
                                    .where(Task.GROUP_IDENTIFIER.exactly().code(medicationRequest.getGroupIdentifier().getValue()))
                                    .and(Task.STATUS.exactly().code("accepted"))
                                    .returnBundle(Bundle.class)
                                    .execute();

                            if (resultBundle.getEntry().size() == 0) {

                                Bundle results = client
                                        .search()
                                        .forResource(MedicationRequest.class)
                                        .where(MedicationRequest.RES_ID.exactly().code(component.getResource().getId()))
                                        .include(MedicationRequest.INCLUDE_ALL.asRecursive())
                                        .returnBundle(Bundle.class)
                                        .execute();
                                if (!results.isEmpty()) {
                                    bundle.addEntry(new Bundle.BundleEntryComponent().setResource(results));


                                    if (medicationRequest != null) {

                                        Task task = new Task();
                                        task.setGroupIdentifier(medicationRequest.getGroupIdentifier());
                                        task.setStatus(Task.TaskStatus.ACCEPTED);
                                        task.setAuthoredOn(new Date());
                                        client.create()
                                                .resource(task)
                                                .conditionalByUrl("Task?group-identifier=" + medicationRequest.getGroupIdentifier().getSystem() + "|" + medicationRequest.getGroupIdentifier().getValue() + "&status=accepted")
                                                .execute();
                                    }
                                }
                            }
                        }


                    }

                }
            }
        }
        return bundle;
    }

}
