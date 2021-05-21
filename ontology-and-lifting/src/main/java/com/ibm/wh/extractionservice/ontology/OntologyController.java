package com.ibm.wh.extractionservice.ontology;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class OntologyController {

    public static final String ENDPOINT__GET_ONTOLOGY = "/ontology";

    private final OntologyService ontologyService;

    @Autowired
    public OntologyController(OntologyService parsingService) {
        this.ontologyService = parsingService;
    }

    @GetMapping(path = ENDPOINT__GET_ONTOLOGY)
    public String getOntology(@RequestParam(value = "syntax", defaultValue = Syntax.TURTLE_AS_STRING) Syntax outputSyntax) {
        return ontologyService.getOntology().toString(outputSyntax);
    }

}
