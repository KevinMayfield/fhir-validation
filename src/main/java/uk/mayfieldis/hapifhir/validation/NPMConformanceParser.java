package uk.mayfieldis.hapifhir.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.utilities.cache.NpmPackage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NPMConformanceParser
{

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(NPMConformanceParser.class);


    public static PrePopulatedValidationSupport getPrePopulatedValidationSupport(FhirContext ctx, NpmPackage npm) throws Exception {

        Map<String, IBaseResource> myCodeSystems = new HashMap();
        Map<String, IBaseResource> myStructureDefinitions = new HashMap();
        Map<String, IBaseResource> myValueSets = new HashMap();



        LOG.info("Loading IG Validation Support {}", npm.getPath());
        for (String resource : npm.listResources( "StructureDefinition")) {

            StructureDefinition structureDefinition = (StructureDefinition) ctx.newJsonParser().parseResource(npm.load("package", resource));
            LOG.debug("Loading: {} fhirVersion {}",structureDefinition.getUrl(), structureDefinition.getFhirVersion().toString());
            if (!structureDefinition.hasSnapshot() && structureDefinition.getDerivation().equals(StructureDefinition.TypeDerivationRule.CONSTRAINT)) {
                LOG.warn("Missing Snapshot {}", structureDefinition.getUrl());
            }
            myStructureDefinitions.put(structureDefinition.getUrl(),structureDefinition);
        }
        for (String resource : npm.listResources("ValueSet")) {
            ValueSet valueSet = (ValueSet) ctx.newJsonParser().parseResource(npm.load("package", resource));
            LOG.debug("Loading: {}", valueSet.getUrl());
            myValueSets.put(valueSet.getUrl(), valueSet);
        }
        for (String resource : npm.listResources("CodeSystem")) {
            CodeSystem codeSys = (CodeSystem) ctx.newJsonParser().parseResource(npm.load("package", resource));
            LOG.debug("Loading: {}", codeSys.getUrl());
            myCodeSystems.put(codeSys.getUrl(), codeSys);
        }

        return new PrePopulatedValidationSupport(ctx,myStructureDefinitions,myValueSets,myCodeSystems);
    }


}
