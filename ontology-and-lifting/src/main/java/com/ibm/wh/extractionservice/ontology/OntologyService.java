package com.ibm.wh.extractionservice.ontology;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OntologyService {

    private final DomainOntology domainOntology;
    private static final Logger logger = LoggerFactory.getLogger(OntologyService.class);

    @Autowired
    public OntologyService(DomainOntology domainOntology) {
        this.domainOntology = domainOntology;
    }

    public DomainOntology getOntology() {
        return domainOntology;
    }

}
