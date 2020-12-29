package uk.mayfieldis.fhirservice;

import ca.uhn.fhir.context.ConfigurationException;
import org.yaml.snakeyaml.Yaml;
import uk.mayfieldis.hapifhir.FHIRServerProperties;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Properties;

public class MessageConfig {

    static final String MESSAGE_CONFIG = "message.yml";

    private static Properties config;

    public static Properties getProperties() {

        Yaml yaml = new Yaml();
        if (config == null) {
            // Load the configurable properties file
            try (InputStream in = FHIRServerProperties.class.getClassLoader().getResourceAsStream(MESSAGE_CONFIG)){
                MessageConfig.config = new Properties();
                config = yaml.loadAs( in, Properties.class );
            } catch (Exception e) {
                throw new ConfigurationException("Could not load Message config", e);
            }

            Properties overrideProps = loadOverrideProperties();
            if(overrideProps != null) {
                config.putAll(overrideProps);
            }
        }
        return config;
    }

    private static Properties loadOverrideProperties() {

        String confFile = System.getProperty(MESSAGE_CONFIG);
        if(confFile != null) {
            Yaml yaml = new Yaml();
            try {
                MessageConfig.config = new Properties();
                config = yaml.loadAs(Files.newInputStream(Paths.get(System.getProperty(MESSAGE_CONFIG) )), Properties.class);
                return config;
            }
            catch (Exception e) {
                throw new ConfigurationException("Could not load Message config file: " + confFile, e);
            }
        }
        return null;
    }

    public static Boolean doUpdate(String propertyName, String message) {
        Properties properties = MessageConfig.getProperties();

        if (properties != null) {
            String[] msg = message.split("\\^");

            if (msg.length>0) {
                // + "." + msg[1]
                Object object = properties.get(propertyName);
                if (object != null && object instanceof LinkedHashMap) {
                    LinkedHashMap linkedMap = (LinkedHashMap) object;
                    if (msg.length == 1 && linkedMap.get(message)!=null) return true;
                    if (linkedMap.get(msg[0]) != null) {
                        Object object2 = linkedMap.get(msg[0]);
                        if (msg.length>1 && object2 != null && object2 instanceof ArrayList) {
                            ArrayList list = (ArrayList<String>) object2;
                            if (list.indexOf(msg[1])>-1) return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
