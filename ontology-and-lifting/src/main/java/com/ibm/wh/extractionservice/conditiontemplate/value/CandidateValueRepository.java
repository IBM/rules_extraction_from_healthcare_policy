package com.ibm.wh.extractionservice.conditiontemplate.value;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.value.CandidateValue;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity.Type;
import com.ibm.wh.extractionservice.externalentity.ExternalEntityRepository;
import com.ibm.wh.extractionservice.ontology.DomainOntology;

@Repository
public class CandidateValueRepository {

    private final DomainOntology domainOntology;

    private final Set<String> groupExternalEntitiesIds;

    @Autowired
    public CandidateValueRepository(ExternalEntityRepository externalEntityRepository,
                                    DomainOntology domainOntology) {
        this.domainOntology = domainOntology;
        this.groupExternalEntitiesIds = getAllGroupExternalEntitiesIds(externalEntityRepository);
    }

    private Set<String> getAllGroupExternalEntitiesIds(ExternalEntityRepository externalEntityRepository) {
        return externalEntityRepository.findByType(Type.GROUP)
                .stream()
                .map(ExternalEntity::getEntityId)
                .collect(Collectors.toSet());
    }

    public Optional<CandidateValue> findById(String id) {
        return domainOntology.getIndividualByUri(id).map(this::toCandidateValue);
    }

    public Optional<CandidateValue> findById(String id, Collection<ExternalEntity> extractedEntities) {
        Optional<CandidateValue> candidateValueInOntology = findById(id);
        if (candidateValueInOntology.isPresent()) return candidateValueInOntology;
        else return findInExtractedEntities(id, extractedEntities);
    }

    private Optional<CandidateValue> findInExtractedEntities(String id, Collection<ExternalEntity> extractedEntities) {
        return extractedEntities.stream()
                .filter(entity -> entity.getEntityId().equals(id))
                .findFirst()
                .map(this::toCandidateValue);
    }

    private CandidateValue toCandidateValue(ExternalEntity extractedEntity) {
        return new CandidateValue(extractedEntity.getEntityId(), extractedEntity.getDisplayName(), extractedEntity.getClaimValue(), CandidateValue.Type.EXTRACTED);
    }

    private CandidateValue toCandidateValue(Individual individual) {
        return new CandidateValue(individual.getURI(), displayNameOf(individual), claimValueOf(individual), typeOf(individual));
    }

    private String displayNameOf(OntResource resource) {
        return domainOntology.getDisplayName(resource)
                .orElse(resource.getLocalName());
    }

    private String claimValueOf(OntResource resource) {
        return domainOntology.getClaimValue(resource);
    }

    private CandidateValue.Type typeOf(Individual individual) {
        return isGroup(individual) ? CandidateValue.Type.GROUP : CandidateValue.Type.SINGLE;
    }

    private boolean isGroup(Individual individual) {
        return groupExternalEntitiesIds.contains(individual.getURI());
    }

}
