FROM amazoncorretto:21
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} goat.jar
EXPOSE 5000
ENTRYPOINT ["java","-jar","/goat.jar"]