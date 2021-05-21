package com.ibm.wh.extractionservice.conditiontemplate;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.AbstractConditionTemplate;
import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.FreeTextConditionTemplate;
import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.MultipleSelectionConditionTemplate;
import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.SingleSelectionConditionTemplate;
import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.value.CandidateValue;
import com.ibm.wh.extractionservice.conditiontemplate.defaultvalue.ConditionTemplateDefaultValue;
import com.ibm.wh.extractionservice.conditiontemplate.value.CandidateValueRepository;
import com.ibm.wh.extractionservice.ontology.DomainOntology;

@Component
public class ConditionTemplateRepository {

    private final CandidateValueRepository candidateValueRepository;
    private final DomainOntology domainOntology;
    private final Collection<AbstractConditionTemplate> allConditionTemplates;

    @Autowired
    public ConditionTemplateRepository(CandidateValueRepository candidateValueRepository,
                                       DomainOntology domainOntology) {
        this.candidateValueRepository = candidateValueRepository;
        this.domainOntology = domainOntology;
        this.allConditionTemplates = getConditionTemplates(domainOntology.getBenefitRuleClasses());
    }

    public Collection<AbstractConditionTemplate> findAll() {
        return this.allConditionTemplates;
    }

    public Set<ConditionTemplateDefaultValue> findAllDefaultValues() {
        Map<Resource, String> allPropertiesWithDefaultValues = this.domainOntology.getAllPropertiesWithDefaultValues();
        return allPropertiesWithDefaultValues.entrySet().stream()
                .map(entry -> new ConditionTemplateDefaultValue(entry.getKey().getURI(), entry.getValue()))
                .collect(Collectors.toSet());
    }

    public  Set<AbstractConditionTemplate> findAnnotationRuleTags () {
        Set<AbstractConditionTemplate> res = new HashSet<> ();
        // get all subtypes of BenefitRules
        Set<OntClass> benefitRuleTypes = this.domainOntology.getBenefitRuleClasses();
        for (OntClass benefitRuleType: benefitRuleTypes) {
            // find all obj property-value of a benefit rule subtype
            Map<Resource, Resource> objPropValueMap = this.domainOntology.getAllObjectPropertiesOfClass(benefitRuleType);
            Set<CandidateValue> candidateValues = new HashSet<>();
            for (Resource objPropValue: objPropValueMap.values()) {
                Optional<String> displayNameValue = this.domainOntology.getDisplayName(objPropValue);
                candidateValues.add(new CandidateValue(objPropValue.getURI(),
                        displayNameValue.orElse(""), null, CandidateValue.Type.SINGLE));
            }

            Optional<String> displayNameRule = this.domainOntology.getDisplayName(benefitRuleType);
            res.add(new SingleSelectionConditionTemplate(benefitRuleType.getURI(),
                    displayNameRule.orElse(""), candidateValues));

        }
        return res;
    }

    private Collection<AbstractConditionTemplate> getConditionTemplates(Collection<OntClass> rootClasses) {
        return rootClasses.stream()
                .map(this::getConditionTemplates)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Collection<AbstractConditionTemplate> getConditionTemplates(OntClass rootClass) {
        // get properties of the class unions that include the current rootClass
        Set<AbstractConditionTemplate> conditionTemplates = getAllClassUnionsWith(rootClass).stream()
                .map(this::getConditionTemplates)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Collection<OntProperty> properties = domainOntology.getObjectAndDataPropertiesDeclaredForClass(rootClass);

        for (OntProperty property : properties) {
            if (domainOntology.isLeafProperty(property)) {
                conditionTemplates.add(toConditionTemplate(property));
            } else {
                OntClass range = domainOntology.getSingleRangeClass(property.asObjectProperty(), true).get();

                if (range.isUnionClass()) {
                    Set<AbstractConditionTemplate> conditionTemplatesOfOperands = range.asUnionClass().getOperands().asJavaList().stream()
                            .filter(node -> node.canAs(OntClass.class))
                            .map(node -> getConditionTemplates(node.as(OntClass.class)))
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
                    conditionTemplates.addAll(conditionTemplatesOfOperands);
                } else {
                    conditionTemplates.addAll(getConditionTemplates(range));
                }
            }
        }

        return conditionTemplates;
    }

    private static Collection<? extends OntClass> getAllClassUnionsWith(OntClass ontClass) {
        return ontClass.getOntModel().listUnionClasses()
                .filterKeep(unionClass -> unionContains(unionClass, ontClass))
                .toSet();
    }

    private static boolean unionContains(UnionClass unionClass, OntClass ontClass) {
        return unionClass.getOperands().asJavaList().stream()
                .anyMatch(unionComponent -> unionComponent.asResource().equals(ontClass));
    }

    private AbstractConditionTemplate toConditionTemplate(OntProperty property) {
        if (property.isDatatypeProperty()) {
            return new FreeTextConditionTemplate(property.getURI(), displayNameOf(property));
        } else if (property.isObjectProperty() && property.isFunctionalProperty()) {
            return new SingleSelectionConditionTemplate(property.getURI(), displayNameOf(property), rangeCandidateValues(property));
        } else if (property.isObjectProperty() && !property.isFunctionalProperty()) {
            return new MultipleSelectionConditionTemplate(property.getURI(), displayNameOf(property), rangeCandidateValues(property));
        } else {
            throw new IllegalStateException(String.format("Unable to create condition template from property %s", property));
        }
    }

    private String displayNameOf(OntResource resource) {
        return domainOntology.getDisplayName(resource)
                .orElse(resource.getLocalName());
    }

    private Set<CandidateValue> rangeCandidateValues(OntProperty property) {
        if (!property.isObjectProperty()) return Collections.emptySet();
        return candidateValues(domainOntology.getSingleRangeClass(property.asObjectProperty(), true).get());
    }

    private Set<CandidateValue> candidateValues(OntClass ontClass) {
        if (ontClass.isUnionClass()) return candidateValuesOfUnionClass(ontClass.asUnionClass());
        return ontClass.listInstances()
                .mapWith(OntResource::getURI)
                .mapWith(candidateValueRepository::findById)
                .filterKeep(Optional::isPresent)
                .mapWith(Optional::get)
                .toSet();
    }

    private Set<CandidateValue> candidateValuesOfUnionClass(UnionClass unionClass) {
        return unionClass.getOperands().asJavaList()
                .stream()
                .filter(node -> node.canAs(OntClass.class))
                .map(node -> candidateValues(node.as(OntClass.class)))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

}
