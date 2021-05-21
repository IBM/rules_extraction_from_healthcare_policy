package com.ibm.wh.extractionservice.ontology.search.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.ibm.wh.extractionservice.ontology.search.OntologySearchResult;

@FunctionalInterface
public interface OntologySearchResultsFilter {

    List<OntologySearchResult> filter(List<OntologySearchResult> ontologySearchResults);

    static List<OntologySearchResult> filterMaxDecreaseThreshold(float maxDecreasePercentage, List<OntologySearchResult> ontologySearchResults) {
        if (maxDecreasePercentage < 0 || maxDecreasePercentage > 1)
            throw new IllegalArgumentException(String.format("[%f] is an invalid percentage, the percentage must be a value between 0 and 1", maxDecreasePercentage));

        List<OntologySearchResult> filteredResults = new ArrayList<>();
        double previousScore = 0;
        if (ontologySearchResults.size() > 0) {
            filteredResults.add(ontologySearchResults.get(0));
            previousScore = filteredResults.get(0).getScore();
        }

        for (int i = 1; i < ontologySearchResults.size() && checkPercentualDecrease(ontologySearchResults.get(i).getScore(), previousScore, maxDecreasePercentage); i++) {
            previousScore = ontologySearchResults.get(i).getScore();
            filteredResults.add(ontologySearchResults.get(i));
        }

        return filteredResults;
    }

    static boolean checkPercentualDecrease(double value, double prevValue, float threeshold) {
        return (prevValue - value) / prevValue < threeshold;
    }

    static OntologySearchResultsFilter buildMaxDecreaseStrategyFilter(BiFunction<Float, List<OntologySearchResult>, List<OntologySearchResult>> maxDecreaseFunction, Float maxDecreasePercentage) {
        return (y) -> maxDecreaseFunction.apply(maxDecreasePercentage, y);
    }

}
