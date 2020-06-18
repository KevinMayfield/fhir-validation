FROM openjdk:11-slim
VOLUME /tmp

ENV JAVA_OPTS="-Xms128m -Xmx4096m"

ADD target/gm-fhir-message.jar gm-fhir-message.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/gm-fhir-message.jar"]


