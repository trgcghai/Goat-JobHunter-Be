FROM eclipse-temurin:21-jre
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} goat.jar
ENTRYPOINT ["java","-jar","/goat.jar"]