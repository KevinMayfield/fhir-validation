package uk.mayfieldis.fhirservice.test;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ValidationTestR4IT {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ValidationTestR4IT.class);
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    FhirContext ctxtest = FhirContext.forR4();

    private static String EPS_EXAMPLES = "eps-examples/";

    private static String SPINE_EXAMPLES = "spine-examples/";

    private static String COVID_EXAMPLES = "covid-examples/";

    private static String FAIL_EXAMPLES = "examples/";

    @TestConfiguration
    static class ValidationServerR4ITContextConfiguration {

    }


    private ResponseEntity postResource(String json, String resourceType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<Object>(json, headers);
        return restTemplate.exchange("http://localhost:" + port + "/R4/"+resourceType, HttpMethod.POST, entity, String.class);
    }

    private ResponseEntity validateBundle(String json, MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);

        HttpEntity<Object> entity = new HttpEntity<Object>(json, headers);
        return restTemplate.exchange("http://localhost:" + port + "/R4/$validate", HttpMethod.POST, entity, String.class);
    }

    private ResponseEntity validateResource(String json, MediaType mediaType, String profile) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);

        HttpEntity<Object> entity = new HttpEntity<Object>(json, headers);
        return restTemplate.exchange("http://localhost:" + port + "/R4/$validate?profile="+profile, HttpMethod.POST, entity, String.class);
    }

    private IBaseResource getFileResourceJSON(String fileName) {
        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        assertNotNull(inputStream);
        Reader reader = new InputStreamReader(inputStream);
        return ctxtest.newJsonParser().parseResource(reader);
    }

    private IBaseResource getFileResourceXML(String fileName) {
        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        assertNotNull(inputStream);
        Reader reader = new InputStreamReader(inputStream);
        return ctxtest.newXmlParser().parseResource(reader);
    }



    @Test
    public void metadataShouldReturnCapabilityStatement() throws Exception {
        assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/R4/metadata",
                String.class)).contains("CapabilityStatement");
    }

    @Test
    public void contextLoads() throws Exception {
    }

    @Test
    public void validateCOVIDObservation() throws Exception {
        log.info("validate COVID Observation");

        IBaseResource resource = getFileResourceJSON(COVID_EXAMPLES + "observation-SARS-CoV-2-ORGY.json");
        ResponseEntity<String> out = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.nhs.uk/StructureDefinition/Spine-Observation");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateCOVIDVaccination1stDose() throws Exception {
        log.info("validate COVID Vaccination1stDose");

        IBaseResource resource = getFileResourceJSON(COVID_EXAMPLES + "immunization-covid.json");
        ResponseEntity<String> out = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.nhs.uk/StructureDefinition/Spine-Immunization");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateCOVIDVaccination2ndDose() throws Exception {
        log.info("validate COVID Vaccination2ndDose");

        IBaseResource resource = getFileResourceJSON(COVID_EXAMPLES + "immunization-covid-2nd-dose.json");
        ResponseEntity<String> out = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.nhs.uk/StructureDefinition/Spine-Immunization");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateCOVIDJackDawkins() throws Exception {
        log.info("validate COVID JackDawkins");

        IBaseResource resource = getFileResourceJSON(COVID_EXAMPLES + "patient-jack-dawkins.json");
        ResponseEntity<String> out = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.nhs.uk/StructureDefinition/Spine-Patient");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateCOVIDRichardSmith() throws Exception {
        log.info("validate COVID RichardSmith");

        IBaseResource resource = getFileResourceJSON(COVID_EXAMPLES + "patient-RichardSmith.json");
        ResponseEntity<String> out = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.nhs.uk/StructureDefinition/Spine-Patient");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validatePrescriptionOrder() throws Exception {
        log.info("validatePrescriptionOrder");

        IBaseResource resource = getFileResourceXML(EPS_EXAMPLES + "Bundle-prescription-order.xml");
        ResponseEntity<String> out = validateBundle(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateDispenseNotificationWithPatient() throws Exception {
        log.info("validateDispenseNotificationWithPatient");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "dispense-1st-event-with-dispenser-with-patient-resource.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateDispenseNotification() throws Exception {
        log.info("validateDispenseNotification");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "dispense-1st-event-with-dispenser.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateGPCommunityPrescription() throws Exception {
        log.info("validateGPCommunityPrescription");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "gp-community-prescription.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validatehomecareexample() throws Exception {
        log.info("validatehomecare-example");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "homecare-continuous-example.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateHomecareExampleWithMedicationResource() throws Exception {
        log.info("validate HomecareExampleWithMedicationResource");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "homecare-example-with-medication-resource.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateopenEPbetterexample() throws Exception {
        log.info("validate openEPbetterexample");

        IBaseResource resource = getFileResourceXML(EPS_EXAMPLES + "openEP-better-example.xml");
        ResponseEntity<String> out = validateBundle(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateoutpatientacuteexample() throws Exception {
        log.info("validate outpatientacuteexample");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "outpatient-acute-example.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateoutpatientendorsedexample() throws Exception {
        log.info("validate outpatientendorsedexample");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "outpatient-endorsed-example.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }


    @Test
    public void validateoutpatientFourItemsexample() throws Exception {
        log.info("validate outpatientFourItemsexample");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "outpatient-four-items-example.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }



    @Test
    public void validateMedicationRequestAlienProfilePass() throws Exception {
        log.info("validate MedicationRequestAlienProfilePass");

        IBaseResource resource = getFileResourceXML(EPS_EXAMPLES + "MedicationRequest-alienProfile-pass.xml");
        ResponseEntity<String> out = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.nhs.uk/StructureDefinition/DM-MedicationRequest");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateMedicationRequestControlledDrug() throws Exception {
        log.info("validate MedicationRequestControlledDrug");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "MedicationRequest-Example-ControlledDrug.json");
        ResponseEntity<String> out = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.nhs.uk/StructureDefinition/DM-MedicationRequest");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateoutpatientfouritemscancelmorphine() throws Exception {
        log.info("validate outpatientfouritemscancelmorphine");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "outpatient-four-items-cancel-morphine.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateoutpatientfouritemscancelresponsemorphine() throws Exception {
        log.info("validate outpatientfouritemscancelresponsemorphine");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "outpatient-four-items-cancel-response-morphine.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateoutpatientfouritemscancelsubsequentresponsemorphine() throws Exception {
        log.info("validate outpatientfouritemscancelsubsequentresponsemorphine");

        IBaseResource resource = getFileResourceJSON(EPS_EXAMPLES + "outpatient-four-items-cancel-subsequent-response-morphine.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateMedicationRequestmissingSNOMEDMedicationCodesFail() throws Exception {
        log.info("validate MedicationRequest missingSNOMEDMedicationCodes fail");

        IBaseResource resource = getFileResourceXML(FAIL_EXAMPLES + "MedicationRequest-missingSNOMEDMedicationCodes-fail.xml");
        ResponseEntity<String> out = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.nhs.uk/StructureDefinition/DM-MedicationRequest");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    public void validateMedicationRequestmultipleMedicationCodesPass() throws Exception {
        log.info("validate MedicationRequest multipleMedicationCodes-pass");

        IBaseResource resource = getFileResourceXML(EPS_EXAMPLES + "MedicationRequest-multipleMedicationCodes-pass.xml");
        ResponseEntity<String> out = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.nhs.uk/StructureDefinition/DM-MedicationRequest");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateMedicationRequestrepeatDispensingPass() throws Exception {
        log.info("validate MedicationRequest repeatDispensing-pass");

        IBaseResource resource = getFileResourceXML(EPS_EXAMPLES + "MedicationRequest-repeatDispensing-pass.xml");
        ResponseEntity<String> out = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.nhs.uk/StructureDefinition/DM-MedicationRequest");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateOrganization() throws Exception {
        log.info("validate Organization");

        IBaseResource resource = getFileResourceXML(SPINE_EXAMPLES + "Organization.xml");
        ResponseEntity<String> out = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.nhs.uk/StructureDefinition/Spine-Organization");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);

        ResponseEntity<String> out2 = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.hl7.org.uk/StructureDefinition/UKCore-Organization");
        log.info(out2.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validatePatientPDS() throws Exception {
        log.info("validate PatientPDS");

        IBaseResource resource = getFileResourceJSON(SPINE_EXAMPLES +"Patient-PDS.json");
        ResponseEntity<String> out = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.nhs.uk/StructureDefinition/Spine-Patient");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);

        ResponseEntity<String> out2 = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.hl7.org.uk/StructureDefinition/UKCore-Patient");
        log.info(out2.getBody());
        assertThat(out2.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validatePatientPDSDM() throws Exception {
        log.info("validate PatientPDS");

        IBaseResource resource = getFileResourceJSON(FAIL_EXAMPLES +"Patient-PDS-fail.json");
        ResponseEntity<String> out = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.nhs.uk/StructureDefinition/Spine-Patient");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.UNPROCESSABLE_ENTITY);

    }

    @Test
    public void validatePatientPDSUKCore() throws Exception {
        log.info("validate PatientPDS");

        IBaseResource resource = getFileResourceJSON(FAIL_EXAMPLES +"Patient-PDS-fail.json");
        
        ResponseEntity<String> out2 = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.hl7.org.uk/StructureDefinition/UKCore-Patient");
        log.info(out2.getBody());
        assertThat(out2.getStatusCode()).isEqualByComparingTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    public void validatePractitioner() throws Exception {
        log.info("validate Practitioner");

        IBaseResource resource = getFileResourceXML(SPINE_EXAMPLES +"Practitioner.xml");
        ResponseEntity<String> out = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.nhs.uk/StructureDefinition/Spine-Practitioner");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);

        ResponseEntity<String> out2 = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.hl7.org.uk/StructureDefinition/UKCore-Practitioner");
        log.info(out2.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }
    @Test
    public void validatePractitionerRole() throws Exception {
        log.info("validate PractitionerRole");

        IBaseResource resource = getFileResourceXML(SPINE_EXAMPLES +"PractitionerRole-pass.xml");
        ResponseEntity<String> out = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.nhs.uk/StructureDefinition/Spine-PractitionerRole");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);

        ResponseEntity<String> out2 = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.hl7.org.uk/StructureDefinition/UKCore-PractitionerRole");
        log.info(out2.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

}
