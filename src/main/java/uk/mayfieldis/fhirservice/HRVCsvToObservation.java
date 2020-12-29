package uk.mayfieldis.fhirservice;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HRVCsvToObservation implements Processor {

    FhirContext ctx = null;

    public HRVCsvToObservation(FhirContext _ctx) {
        this.ctx = _ctx;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRVCsvToObservation.class);


    @Override
    public void process(Exchange exchange) throws Exception {

        // HAPI needs a transaction bundle to process the resources (if we posted the message bundle in we would get a bundle stored)

        Object body = exchange.getIn().getBody();
        Bundle bundle= null;
        if (body instanceof InputStream) {

            bundle = new Bundle();
            CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new InputStreamReader((InputStream) body));
            for (CSVRecord record : csvParser) {
                String timestamp= record.get("timestamp_measurement");
                String SDNN = record.get("SDNN");
                String recovery = record.get("HRV4T_Recovery_Points");
                String vo2max = "";
                try {
                    vo2max = record.get(" vo2max");
                 //   log.info(vo2max);
                } catch (Exception ex) {
                    log.info(ex.getMessage());
                }

                if (!timestamp.isEmpty() && timestamp.length() > 1 ) {
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
                        observation.addIdentifier(identifer(observation,date));
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
                        observation.addIdentifier(identifer(observation,date));
                        bundle.addEntry().setResource(observation);
                    }
                    if (!vo2max.isBlank() ) {

                        Double value = Double.parseDouble(vo2max);
                        if (value> 0) {

                            Observation observation = new Observation();
                            observation.setEffective(new DateTimeType(date));
                            observation.setValue(new Quantity()
                                    .setValue(value)
                                    .setUnit("ml/min")
                            );
                            //http://emt.bme.hu/emt/sites/emt.bme.hu.emt/files/HL7-FHIR-ADL-WhitePaper-Concerto.pdf
                            observation.setCode(
                                    new CodeableConcept().addCoding(
                                            new Coding()
                                                    .setSystem("http://loinc.org")
                                                    .setCode("60842-2")
                                                    .setDisplay("Oxygen consumption (VO2)")
                                    ));
                            observation.addIdentifier(identifer(observation,date));
                            bundle.addEntry().setResource(observation);
                        }
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

    private Identifier identifer(Observation observation, Date date) {
        return new Identifier()
                .setSystem("https://www.hrv4training.com/Id")
                .setValue(observation.getCode().getCodingFirstRep().getCode() + "-"+ Long.toString(date.getTime()) );
    }

}
