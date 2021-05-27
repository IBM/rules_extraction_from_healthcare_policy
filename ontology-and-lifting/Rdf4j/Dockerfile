FROM tomcat:9

ENV RDF4J_VERSION="3.6.3"
ENV RDF4J_DATA="/opt/eclipse-rdf4j-${RDF4J_VERSION}/data" 
ENV JVM_PARAMS="-Xmx4g" 

RUN curl -sS -o /tmp/rdf4j.zip -L http://download.eclipse.org/rdf4j/eclipse-rdf4j-${RDF4J_VERSION}-sdk.zip && \
    cd /opt && \ 
    unzip /tmp/rdf4j.zip && \
    rm /tmp/rdf4j.zip

RUN mv /opt/eclipse-rdf4j-${RDF4J_VERSION}/war/*.war /usr/local/tomcat/webapps

RUN echo "CATALINA_OPTS=\"\$CATALINA_OPTS \$JVM_PARAMS -Dorg.eclipse.rdf4j.appdata.basedir=\$RDF4J_DATA\"" >> /usr/local/tomcat/bin/setenv.sh

VOLUME ${RDF4J_DATA}

EXPOSE 8080

WORKDIR /app
COPY src/main/resources/ontology/populated-ontology.ttl .
COPY Rdf4j/rdf4j-populate.sh .
COPY Rdf4j/rdf4j-repo-definition.rdf .

############################################################
CMD bash rdf4j-populate.sh & catalina.sh run
############################################################
