package com.ibm.wh.extractionservice.ontology.normalisation;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

public interface NormalisationStage {

    void apply(OntModel ontModel);

    static void applyUpdate(Statement statement, String newObject, UpdateMode updateMode) {
        if (statement.getObject().toString().equals(newObject)) return;
        switch (updateMode) {
            case ADD:
                addStatement(statement.getSubject(), statement.getPredicate(), newObject);
                break;
            case REPLACE:
                replaceStatementsObject(statement, newObject);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown update mode: %s", updateMode));
        }
    }

    static void replaceStatementsObject(Statement statement, String newObject) {
        statement.getModel().remove(statement);
        statement.getModel().add(statement.getSubject(), statement.getPredicate(), newObject);
    }

    static void addStatement(Resource subject, Property predicate, String object) {
        subject.getModel().add(subject, predicate, object);
    }

    enum UpdateMode {
        REPLACE, ADD
    }

}
