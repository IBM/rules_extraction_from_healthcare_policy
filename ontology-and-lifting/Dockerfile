FROM openjdk:8u181-jdk-slim-stretch
WORKDIR /app
COPY gradlew gradlew
RUN ./gradlew --version
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle

########################################
COPY src src
RUN LOGGING_FILE=app.log ./gradlew buildDependents buildNeeded build -x test || { cat app.log; exit 1; }

############################################################
CMD ./gradlew bootRun -x build -x buildDependents -x buildNeeded
############################################################
