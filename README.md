# FHIR Validation

## Getting Started

It also performs FHIR Message serverFHIRValidation.


To you will first need JDK 11 or higher and maven installed on your computer. 
* https://adoptopenjdk.net/
* https://maven.apache.org/index.html

To build the service use this command:  

`mvn build install`

Tu run the service use this command:

`mvn spring-boot:run`

Check the service is running via (on windows). Either:

`powershell -Command "(new-object net.webclient).DownloadString('http://localhost:8187/R4/metadata')`

`curl http://localhost:8187/R4/metadata`

The `$validate` operation will now be available on `http://localhost:8187/R4/$validate`. See https://www.hl7.org/fhir/validation.html#op for instructions.

## Using the open source HAPI FHIR libraries

This validation service was built using the HAPI FHIR Libraries and is based on the [HAPI FHIR Validation](https://hapifhir.io/hapi-fhir/docs/validation/introduction.html) 


