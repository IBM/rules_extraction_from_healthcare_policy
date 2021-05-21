package com.ibm.wh.extractionservice.ontology;

public enum Syntax {

    JSONLD, N3, NTRIPLES, RDFXML, TURTLE;

    public static final String JSONLD_AS_STRING = "JSONLD";
    public static final String N3_AS_STRING = "N3";
    public static final String NTRIPLES_AS_STRING = "NTRIPLES";
    public static final String RDFXML_AS_STRING = "RDFXML";
    public static final String TURTLE_AS_STRING = "TURTLE";

    public static Syntax fromName(String name) {
        switch (name) {
            case JSONLD_AS_STRING:
                return JSONLD;
            case N3_AS_STRING:
                return N3;
            case NTRIPLES_AS_STRING:
                return NTRIPLES;
            case RDFXML_AS_STRING:
                return RDFXML;
            case TURTLE_AS_STRING:
                return TURTLE;
            default:
                throw new IllegalArgumentException(String.format("Syntax name [%s] not found", name));
        }
    }

    public String getName() {
        switch (this) {
            case JSONLD:
                return JSONLD_AS_STRING;
            case N3:
                return N3_AS_STRING;
            case NTRIPLES:
                return NTRIPLES_AS_STRING;
            case RDFXML:
                return RDFXML_AS_STRING;
            case TURTLE:
                return TURTLE_AS_STRING;
            default:
                throw new IllegalStateException(String.format("Something bad happened, syntax [%s] is not well defined!", this));
        }
    }

}
