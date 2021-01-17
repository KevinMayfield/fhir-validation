package uk.mayfieldis.fhirservice.processor;

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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class IHealthCsvToObservation implements Processor {

    FhirContext ctx = null;

    public IHealthCsvToObservation(FhirContext _ctx) {
        this.ctx = _ctx;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IHealthCsvToObservation.class);


    @Override
    public void process(Exchange exchange) throws Exception {

        // HAPI needs a transaction bundle to process the resources (if we posted the message bundle in we would get a bundle stored)

        Object body = exchange.getIn().getBody();
        Bundle bundle= null;
        if (body instanceof InputStream) {

            bundle = new Bundle();
            CSVParser csvParser = null;

            csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new InputStreamReader((InputStream) body));

            if (csvParser.getHeaderMap().size() < 2) {
                InputStream inputStream = (InputStream) body;
                inputStream.reset();
                Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_16);
                csvParser = new CSVParser(reader, CSVFormat.TDF.withFirstRecordAsHeader());
            }
            log.debug(csvParser.getHeaderMap().toString());
            for (CSVRecord record : csvParser.getRecords()) {
                log.trace("Records");
                String timestamp = record.get("Time");
                String spo2 = record.get("SpO2");
                String PI = "";
                try {
                    PI = record.get("PI");
                } catch (Exception ex) {
                    // No Pie
                }
                String dateStr= "";
                try {
                    dateStr = record.get("Date");
                } catch (Exception ex) {
                    // No Pie
                }
                if (!dateStr.isEmpty() && !timestamp.isEmpty()) {
                    timestamp = dateStr + " " + timestamp + ":00";

                }
                if (!timestamp.isEmpty() && timestamp.length() > 1 ) {
                    Date date =new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(timestamp);
                    if (!spo2.isEmpty() ) {
                        Observation observation = new Observation();
                        observation.setEffective(new DateTimeType(date));
                        observation.setValue(new Quantity()
                                .setValue(Double.parseDouble(spo2.replace("%","")))
                                .setUnit("%")
                        );

                        observation.setCode(
                                new CodeableConcept().addCoding(
                                        new Coding()
                                                .setSystem("http://snomed.info/sct")
                                        .setCode("103228002")
                                        .setDisplay("Blood oxygen saturation")
                                ));
                        observation.addIdentifier(identifer(observation,date));
                        bundle.addEntry().setResource(observation);
                    }
                    if (!PI.isEmpty() ) {
                        Observation observation = new Observation();
                        observation.setEffective(new DateTimeType(date));
                        observation.setValue(new Quantity()
                                .setValue(Double.parseDouble(PI))
                                .setUnit("ratio")
                        );

                        observation.setCode(
                                new CodeableConcept().addCoding(
                                        new Coding()
                                                .setSystem("http://loinc.org")
                                                .setCode("73794-0")
                                                .setDisplay("Perfusion index Blood Postductal Pulse oximetry")
                                ));
                        observation.addIdentifier(identifer(observation,date));
                        bundle.addEntry().setResource(observation);
                    }
                }
            }
        }
        else if (body instanceof String) {

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
                .setSystem("https://ihealthlabs.eu/Id")
                .setValue(observation.getCode().getCodingFirstRep().getCode() + "-"+ Long.toString(date.getTime()) );
    }
}
