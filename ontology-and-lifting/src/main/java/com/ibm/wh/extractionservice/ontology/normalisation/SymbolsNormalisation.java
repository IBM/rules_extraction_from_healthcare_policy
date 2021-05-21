package com.ibm.wh.extractionservice.ontology.normalisation;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

public class SymbolsNormalisation implements NormalisationStage {

    private final Property datatypeProperty;
    private final UpdateMode updateMode;

    public SymbolsNormalisation(Property datatypeProperty, UpdateMode updateMode) {
        this.datatypeProperty = datatypeProperty;
        this.updateMode = updateMode;
    }

    @Override
    public void apply(OntModel ontModel) {
        ontModel.listStatements(null, datatypeProperty, (RDFNode) null)
                .toList()
                .forEach(statement -> NormalisationStage.applyUpdate(statement, normalise(statement.getObject().toString()), updateMode));
    }

    private String normalise(String string) {
        return string
                .replace(" / ", "/")
                .replace("/", " / ")
                .replace(" - ", "-")
                .replace("-", " - ");
    }

}
