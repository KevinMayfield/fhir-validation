package uk.nhsd.apim.fhirvalidator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.units.qual.C;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CsvToObservation  implements Processor {

    FhirContext ctx = null;

    public CsvToObservation(FhirContext _ctx) {
        this.ctx = _ctx;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CsvToObservation.class);


    @Override
    public void process(Exchange exchange) throws Exception {

        // HAPI needs a transaction bundle to process the resources (if we posted the message bundle in we would get a bundle stored)

        Object body = exchange.getIn().getBody();
        Bundle bundle= null;
        if (body instanceof InputStream) {
            log.info("InputStream");
            bundle = new Bundle();
            CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new InputStreamReader((InputStream) body));
            for (CSVRecord record : csvParser) {
                String timestamp= record.get("timestamp_measurement");
                String SDNN = record.get("SDNN");
                String recovery = record.get("HRV4T_Recovery_Points");

                if (!timestamp.isEmpty() && timestamp.length() > 1 ) {
                    log.info(timestamp + " - "+ SDNN);
                    Date date =new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(timestamp);
                    if (!SDNN.isEmpty() ) {
                        Observation observation = new Observation();
                        observation.setEffective(new DateTimeType(date));
                        observation.setValue(new Quantity()
                                .setValue(Double.parseDouble(SDNN))
                                .setUnit("SDNN")
                        );
                        //http://emt.bme.hu/emt/sites/emt.bme.hu.emt/files/HL7-FHIR-ADL-WhitePaper-Concerto.pdf
                        observation.setCode(
                                new CodeableConcept().addCoding(
                                        new Coding()
                                                .setSystem("http://loinc.org")
                                        .setCode("8867-4")
                                        .setDisplay("Pulse variation")
                                ));
                        bundle.addEntry().setResource(observation);
                    }
                    if (!recovery.isEmpty() ) {
                        Observation observation = new Observation();
                        observation.setEffective(new DateTimeType(date));
                        observation.setValue(new Quantity()
                                .setValue(Double.parseDouble(recovery))
                                .setUnit("points")
                        );
                        //http://emt.bme.hu/emt/sites/emt.bme.hu.emt/files/HL7-FHIR-ADL-WhitePaper-Concerto.pdf
                        observation.setCode(
                                new CodeableConcept().addCoding(
                                        new Coding()
                                                .setSystem("https://www.hrv4training.com/")
                                                .setCode("Recovery_Points")
                                                .setDisplay("Recovery Points")
                                ));
                        bundle.addEntry().setResource(observation);
                    }
                }
            }
        }
        else if (body instanceof String) {
            log.info("string");
            String string = (String) body;
            IBaseResource resource = ctx.newJsonParser().parseResource(string);
            if (resource instanceof Bundle) {
                bundle = (Bundle) resource;
            }
        } else {
            log.info(body.getClass().getCanonicalName());
        }
        if (bundle == null) {
            throw new UnprocessableEntityException("Empty Message or unknown type");
        }
        bundle.setType(Bundle.BundleType.COLLECTION);

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {

        }

        exchange.getIn().setBody(ctx.newJsonParser().encodeResourceToString(bundle));
    }

}
