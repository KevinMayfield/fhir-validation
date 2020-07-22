package uk.nhsd.apim.fhirvalidator.test;

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
                Thread.currentThread().getContextClassLoader().getResourceAsStream("examples/"+fileName);
        assertNotNull(inputStream);
        Reader reader = new InputStreamReader(inputStream);
        return ctxtest.newJsonParser().parseResource(reader);
    }

    private IBaseResource getFileResourceXML(String fileName) {
        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("examples/"+fileName);
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
    public void validatePrescriptionOrder() throws Exception {
        log.info("validatePrescriptionOrder");

        IBaseResource resource = getFileResourceXML("Bundle-prescription-order.xml");
        ResponseEntity<String> out = validateBundle(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validate3C2366B810010A409U() throws Exception {
        log.info("validate3C2366-B81001-0A409U");

        IBaseResource resource = getFileResourceJSON("3C2366-B81001-0A409U.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validatehomecareexample() throws Exception {
        log.info("validatehomecare-example");

        IBaseResource resource = getFileResourceJSON("homecare-example.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateopenEPbetterexample() throws Exception {
        log.info("validate openEPbetterexample");

        IBaseResource resource = getFileResourceXML("openEP-better-example.xml");
        ResponseEntity<String> out = validateBundle(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateoutpatient1example() throws Exception {
        log.info("validate outpatient1example");

        IBaseResource resource = getFileResourceJSON("outpatient-1-example.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateoutpatient1bexample() throws Exception {
        log.info("validate outpatient1bexample");

        IBaseResource resource = getFileResourceJSON("outpatient-1b-example.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateoutpatient4example() throws Exception {
        log.info("validate outpatient4example");

        IBaseResource resource = getFileResourceJSON("outpatient-4-example.json");
        ResponseEntity<String> out = validateBundle(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON);
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validatePatientEPS() throws Exception {
        log.info("validate PatientEPS");

        IBaseResource resource = getFileResourceJSON("Patient-EPS-pass.json");
        ResponseEntity<String> out = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.nhs.uk/R4/StructureDefinition/DM-Patient");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateMedicationRequestAlienProfilePass() throws Exception {
        log.info("validate MedicationRequestAlienProfilePass");

        IBaseResource resource = getFileResourceXML("MedicationRequest-alienProfile-pass.xml");
        ResponseEntity<String> out = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.nhs.uk/R4/StructureDefinition/DM-MedicationRequest");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateMedicationRequestControlledDrug() throws Exception {
        log.info("validate MedicationRequestControlledDrug");

        IBaseResource resource = getFileResourceJSON("MedicationRequest-Example-ControlledDrug.json");
        ResponseEntity<String> out = validateResource(ctxtest.newJsonParser().encodeResourceToString(resource),MediaType.APPLICATION_JSON, "https://fhir.nhs.uk/R4/StructureDefinition/DM-MedicationRequest");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateMedicationRequestmissingSNOMEDMedicationCodesFail() throws Exception {
        log.info("validate MedicationRequest missingSNOMEDMedicationCodes fail");

        IBaseResource resource = getFileResourceXML("MedicationRequest-missingSNOMEDMedicationCodes-fail.xml");
        ResponseEntity<String> out = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.nhs.uk/R4/StructureDefinition/DM-MedicationRequest");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    public void validateMedicationRequestmultipleMedicationCodesPass() throws Exception {
        log.info("validate MedicationRequest multipleMedicationCodes-pass");

        IBaseResource resource = getFileResourceXML("MedicationRequest-multipleMedicationCodes-pass.xml");
        ResponseEntity<String> out = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.nhs.uk/R4/StructureDefinition/DM-MedicationRequest");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

    @Test
    public void validateMedicationRequestrepeatDispensingPass() throws Exception {
        log.info("validate MedicationRequest repeatDispensing-pass");

        IBaseResource resource = getFileResourceXML("MedicationRequest-repeatDispensing-pass.xml");
        ResponseEntity<String> out = validateResource(ctxtest.newXmlParser().encodeResourceToString(resource),MediaType.APPLICATION_XML, "https://fhir.nhs.uk/R4/StructureDefinition/DM-MedicationRequest");
        log.info(out.getBody());
        assertThat(out.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    }

}
