package com.ibm.wh.extractionservice;

import static com.google.common.collect.Sets.*;
import static com.ibm.wh.extractionservice.support.jena.Models.*;

import java.util.List;
import java.util.Set;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.vocabulary.RDFS;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity;
import com.ibm.wh.extractionservice.externalentity.ExternalEntityService;
import com.ibm.wh.extractionservice.ontology.DomainOntology;
import com.ibm.wh.extractionservice.ontology.normalisation.NormalisationStage;
import com.ibm.wh.extractionservice.ontology.search.filter.OntologySearchResultsFilter;

@Configuration
public class DomainOntologyConfiguration {

    @Value("${ontology.input.files}")
    private String[] domainOntologyInputFiles;
    @Value("${ontology.input.files.syntax}")
    private String domainOntologyInputFileSyntax;
    @Value("${ontology.namespace}")
    private String domainOntologyNamespace;

    @Value("${ontology.annotation.display-name.uri}")
    private String displayNamePropertyUri;
    @Value("${ontology.annotation.claim-value.uri}")
    private String claimValuePropertyUri;
    @Value("${ontology.annotation.surface-form.uri}")
    private String surfaceFormPropertyUri;
    @Value("${ontology.annotation.affirmative-action.uri}")
    private String affirmativeActionPropertyUri;
    @Value("${ontology.annotation.default-value.uri}")
    private String defaultValueUri;
    @Value("${ontology.annotation.negative-action.uri}")
    private String negativeActionPropertyUri;
    @Value("${ontology.annotation.is-negative-property.uri}")
    private String isNegatedPropertyUri;
    @Value("${ontology.annotation.annotated-as.uri}")
    private String annotatedAsPropertyUri;
    @Value("${ontology.annotation.extraction-pattern.uri}")
    private String extractionPatternPropertyUri;
    @Value("${ontology.annotation.has-compliance-rule.uri}")
    private String hasComplianceRulePropertyTagUri;
    @Value("${ontology.benefit-rule.class.uri}")
    private String parentBenefitRuleClassUri;
    @Value("${ontology.policy.class.uri}")
    private String policyClassUri;

    @Bean
    public DomainOntology domainOntology(
            Set<ExternalEntity> externalEntities,
            List<NormalisationStage> normalisationStages
    ) {
        return new DomainOntology.Builder(newHashSet(domainOntologyInputFiles), domainOntologyInputFileSyntax, domainOntologyNamespace)
                .withExternalEntities(externalEntities)
                .withParentBenefitRuleClassUri(parentBenefitRuleClassUri)
                .withDisplayNamePropertyUri(displayNamePropertyUri)
                .withClaimValuePropertyUri(claimValuePropertyUri)
                .withIsNegatedPropertyUri(isNegatedPropertyUri)
                .withAnnotatedAsPropertyUri(annotatedAsPropertyUri)
                .withAffirmativeActionPropertyUri(affirmativeActionPropertyUri)
                .withNegativeActionPropertyUri(negativeActionPropertyUri)
                .withSurfaceFormPropertyUri(surfaceFormPropertyUri)
                .withExtractionPatternPropertyUri(extractionPatternPropertyUri)
                .withDefaultValuePropertyUri(defaultValueUri)
                .withPolicyClassUri(policyClassUri)
                .withHasComplianceRulePropertyTagUri(hasComplianceRulePropertyTagUri)
                .withNormalisationStages(normalisationStages)
                .build();
    }

    @Bean
    public Set<ExternalEntity> externalEntities(ExternalEntityService externalEntityService) {
        return (Set<ExternalEntity>) externalEntityService.findAllEntities();
    }

    @Bean(name = "domainOntologyOntModel")
    public OntModel domainOntologyOntModel(DomainOntology domainOntology) {
        return domainOntology.getOntModel();
    }

    // LuceneOntologyIndexSearch configuration

    @Bean
    public Analyzer analyzer() {
        return new EnglishAnalyzer();
    }

    @Bean(name = "urisOfIndexablePropertiesForLuceneIndex")
    public Set<String> urisOfIndexablePropertiesForLuceneIndex() {
        return Sets.newHashSet(RDFS.label.getURI());
    }

    @Bean("maxDecreaseOntologySearchResultsFilterStrategy")
    public OntologySearchResultsFilter maxDecreaseOntologySearchResultsFilterStrategy() {
        return OntologySearchResultsFilter.buildMaxDecreaseStrategyFilter(
                OntologySearchResultsFilter::filterMaxDecreaseThreshold, 0.3f);
    }

}
