FROM maven:3.5-jdk-8-alpine

ENV APP_FOLDER=/opt/app
RUN mkdir -p $APP_FOLDER
WORKDIR $APP_FOLDER

COPY pom.xml $APP_FOLDER
COPY core/pom.xml $APP_FOLDER/core/pom.xml
COPY ui.apps/pom.xml $APP_FOLDER/ui.apps/pom.xml
COPY ui.content.styla/pom.xml $APP_FOLDER/ui.content.styla/pom.xml
RUN mvn --fail-never dependency:go-offline

COPY . $APP_FOLDER

# actual compilation done at run-time and not during image build. This is to enable volume
# support to get the compiled files availabe on the host
CMD mvn package
