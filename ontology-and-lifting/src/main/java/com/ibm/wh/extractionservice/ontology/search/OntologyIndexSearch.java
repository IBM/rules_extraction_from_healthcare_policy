package com.ibm.wh.extractionservice.ontology.search;

import java.util.List;

public interface OntologyIndexSearch {

    List<OntologySearchResult> search(String queryString, int maxNumberOfResults);

    void close();

}
