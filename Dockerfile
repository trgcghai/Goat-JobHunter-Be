FROM openjdk:21
WORKDIR /app
COPY build/libs/*.jar goat.jar
EXPOSE 5000
CMD ["java","-jar","goat.jar"]