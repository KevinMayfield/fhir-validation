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

## Package Configuration

The `hapi.properties` contains the settings for the validator.

| Setting | Default | Notes |
|--
| validate.flag | true || 
|server.ig.package | UK.DM.r4 ||
|server.ig.version | 0.0.6-dev ||
|server.ig.url | https://packages.simplifier.net/UK.DM.r4/-/UK.DM.r4-0.0.6-dev.tgz ||
|validation.ig.package | UK.Core.r4 ||
|validation.ig.version | 1.1.0 ||
|validation.ig.url | https://packages.simplifier.net/UK.Core.r4/-/UK.Core.r4-1.1.0.tgz ||
|terminology.validation.flag |  true ||
|terminology.server | https://r4.ontoserver.csiro.au/fhir ||
|terminology.snomed.version | http://snomed.info/sct/999000031000000106/version/20200610 ||

