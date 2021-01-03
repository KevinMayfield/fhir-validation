FROM openjdk:11-slim
VOLUME /tmp

ENV JAVA_OPTS="-Xms128m -Xmx512m"

ADD target/fhir-service.jar fhir-service.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/fhir-service.jar"]


