# FHIR  Service


openssl s_client -connect cognito-idp.eu-west-2.amazonaws.com:443 \
| sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > cognito.cer

keytool -importcert -file cognito.cer -keystore keystore.jks -alias "Cognito"

openssl s_client -connect openapi.ihealthlabs.eu:443 \
| sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ihealth.cer

keytool -importcert -file ihealth.cer -keystore keystore.jks -alias "IHealth" 

openssl s_client -connect fhir.mayfield-is.co.uk:443 \
| sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > fhir.cer

keytool -importcert -file fhir.cer -keystore keystore.jks -alias "fhir" 