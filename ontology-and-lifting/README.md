# Ontology and Entity Lifting

*Tools for protecting vital healthcare programs by extracting actionable knowledge from policy.*

This code was originally used as part of a pipeline for Policy Knowledge Extraction, which aims to make a
policy-compliance ‘landscape’ visible to healthcare programs - helping policy investigators identify Fraud, Waste or Abuse. 

This pipeline ingests PDF policy documents and processes them with their metadata, transforming the documents into
a hierarchical tree structure. It performs concept annotation, rule extraction and consolidation guided by reasoning
and checking consistency over the [domain ontology](/ontology-and-lifting/docs/ontology/domain-ontology.md). The ontology guides the transformation of textual patterns into a
semantically-meaningful knowledge graph of rules. 

For more information on the pipeline, the ontology lifting and examples see: 

Towards protecting vital healthcare programs by extracting actionable knowledge from policy. Vanessa Lopez, Nagesh Yadav, Gabriele Picco, Inge Vejsbjerg, Eoin Carroll, Seamus Brady, Marco Luca Sbodio, Lam Thanh Hoang, Miao Wei, John Segrave. Findings of ACL, 2021

Futher information on the domain ontology can be found in:

- [Lopez, Vanessa & Rho, Valentina & Brisimi, Theodora & Segrave-Daly, John & Kristiansen, Morten & Cucci, Fabrizio. (2019). Benefit graph extraction from healthcare policies. ISWC 2019](https://www.researchgate.net/publication/334573678_Benefit_graph_extraction_from_healthcare_policies)

## How to use

An base ontology is provided which can be supplemented by domain-specific external entities. Follow the instructions in
the sections below to generate the ontology and access it though a REST service.

1. [Run the application](#run-the-application)
2. [Add external entities to the ontology](#add-external-entities-to-the-ontology)
3. [Restart and rebuild ontology](#restart-and-rebuild-ontology)
4. [Access the REST API](#access-the-rest-api)

### Run the application
Navigate to the folder `rules_extraction_from_healthcare_policy/ontology-and-lifting/` and run the following docker command.

`docker build --tag policy-knowledge-extraction-service . && docker run -p 8086:8086 policy-knowledge-extraction-service`

### Add external entities to the ontology
The policy knowledge extraction system adds domain-specific external entities such as places of service, CPT and HCPCS codes to the ontology.
For information and full instructions about lifting external data to be used during the extraction process, refer to [this document](/ontology-and-lifting/docs/external-entities-and-lifting.md). This process is required, for example, to
load a new set of procedure codes in the domain ontology. Some predefined configuration files with corresponding csv
files are available in `external-data/*` folders.

### Restart and Rebuild ontology
Stop the docker container and then rebuild your docker image:
`docker build --tag policy-knowledge-extraction-service . && docker run -p 8086:8086 policy-knowledge-extraction-service`
Verify that it is running by accessing http://localhost:8086/swagger-ui.html#/

### Access the REST API
Swagger API documentation should now be available at http://localhost:8086/swagger-ui.html#/

### Export the ontology
The ontology can be exported using the Swagger API at http://localhost:8086/swagger-ui.html#/ontology-controller/getOntologyUsingGET
in JSONLD, N3, NTRIPLES, RDFXML, or TURTLE format.

### Running an RDF4j instance to explore the ontology
You can use RDFj for example to explore the updated ontology file:
`docker build -f Rdf4j/DockerfileRdf4J -t rdf4j . && docker run -p 8080:8080 rdf4j`
RDF4J Workbench is available at http://localhost:8080/rdf4j-workbench and contains the populated ontology that can be obtained using the lifting endpoints described above.
