package com.ibm.wh.extractionservice.ontology;

import static com.github.jsonldjava.shaded.com.google.common.collect.Sets.*;
import static java.util.stream.Collectors.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ibm.wh.extractionservice.support.jena.Models;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.reasoner.ValidityReport.Report;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity;
import com.ibm.wh.extractionservice.commons.externalentity.type.DiscoveredExternalEntity;
import com.ibm.wh.extractionservice.commons.utils.CollectionsUtils;
import com.ibm.wh.extractionservice.ontology.normalisation.NormalisationStage;
import com.ibm.wh.extractionservice.support.StreamUtils;
import com.ibm.wh.extractionservice.support.jena.OntModelAdapter;

public class DomainOntology extends OntModelAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DomainOntology.class);

    private static final boolean RDFS_INFERENCE_ENABLED = true;

    private final OntClass parentBenefitRuleClass;
    private final Map<OntClass, Set<OntProperty>> propertiesToBeExtractedPerBenefitRuleSubtype;
    private final Property displayNameProperty;
    private final Property claimValueProperty;
    private final Property surfaceFormProperty;
    private final OntClass policyClass;
    private final Property isNegatedProperty;
    private final Property annotatedAsProperty;
    private final Property affirmativeActionProperty;
    private final Property negativeActionProperty;
    private final Property extractionPatternProperty;
    private final Property defaultValueProperty;
    private final Optional<Property> hasComplianceRuleProperty;
    // we save discovered entities that are added in the ontology
    // this is needed for WX to work without too many changes, may be removed
    // when refactoring it.
    private final Map<String, DiscoveredExternalEntity> discoveredExternalEntities;

    private DomainOntology(Collection<String> ontologyFilePaths,
                           String ontologySyntax,
                           String namespace,
                           Collection<ExternalEntity> externalEntities,
                           String parentBenefitRuleClassUri,
                           String displayNamePropertyUri,
                           String claimValuePropertyUri,
                           String isNegatedPropertyUri,
                           String annotatedAsPropertyUri,
                           String affirmativeActionPropertyUri,
                           String negativeActionPropertyUri,
                           String surfaceFormPropertyUri,
                           String extractionPatternPropertyUri,
                           String defaultValuePropertyUri,
                           String hasComplianceRulePropertyUri,
                           String policyClassUri,
                           List<NormalisationStage> normalisationStages
    ) {
        super(ontologyFilePaths, ontologySyntax, namespace, RDFS_INFERENCE_ENABLED);
        this.isNegatedProperty = ResourceFactory.createProperty(isNegatedPropertyUri);
        this.annotatedAsProperty = ResourceFactory.createProperty(annotatedAsPropertyUri);
        this.affirmativeActionProperty = ResourceFactory.createProperty(affirmativeActionPropertyUri);
        this.negativeActionProperty = ResourceFactory.createProperty(negativeActionPropertyUri);
        this.extractionPatternProperty = ResourceFactory.createProperty(extractionPatternPropertyUri);
        this.defaultValueProperty = ResourceFactory.createProperty(defaultValuePropertyUri);
        this.hasComplianceRuleProperty = Optional.ofNullable(ResourceFactory.createProperty(hasComplianceRulePropertyUri));
        this.policyClass = getClassByUri(policyClassUri)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Class <%s> not defined in the domain ontology", policyClassUri)));

        this.displayNameProperty = ResourceFactory.createProperty(displayNamePropertyUri);
        this.claimValueProperty = ResourceFactory.createProperty(claimValuePropertyUri);
        this.surfaceFormProperty = ResourceFactory.createProperty(surfaceFormPropertyUri);

        this.parentBenefitRuleClass = getClassByUri(parentBenefitRuleClassUri)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Class <%s> not defined in the domain ontology", parentBenefitRuleClassUri)));

        this.propertiesToBeExtractedPerBenefitRuleSubtype = getAllPropertiesToBeExtracted(parentBenefitRuleClass);

        populateWithExternalEntities(externalEntities);
        normalise(normalisationStages);
        validate();

        this.discoveredExternalEntities = new HashMap<>();
    }

    // constructor used to create a clone
    private DomainOntology(OntModel model,
                           String baseNamespace,
                           OntClass parentBenefitRuleClass,
                           Map<OntClass, Set<OntProperty>> propertiesToBeExtractedPerBenefitRuleSubtype,
                           Property displayNameProperty,
                           Property claimValueProperty,
                           Property surfaceFormProperty,
                           OntClass policyClass,
                           Property isNegatedProperty,
                           Property annotatedAsProperty,
                           Property affirmativeActionProperty,
                           Property negativeActionProperty,
                           Property extractionPatternProperty,
                           Property defaultValueProperty,
                           Optional<Property> hasComplianceRuleProperty) {
        super(model, baseNamespace, RDFS_INFERENCE_ENABLED);
        this.isNegatedProperty = ResourceFactory.createProperty(isNegatedProperty.getURI());
        this.annotatedAsProperty = ResourceFactory.createProperty(annotatedAsProperty.getURI());
        this.affirmativeActionProperty = ResourceFactory.createProperty(affirmativeActionProperty.getURI());
        this.negativeActionProperty = ResourceFactory.createProperty(negativeActionProperty.getURI());
        this.extractionPatternProperty = ResourceFactory.createProperty(extractionPatternProperty.getURI());
        this.defaultValueProperty = ResourceFactory.createProperty(defaultValueProperty.getURI());
        this.hasComplianceRuleProperty = Optional.ofNullable(ResourceFactory.createProperty(hasComplianceRuleProperty.get().getURI()));
        this.policyClass = getClassByUri(policyClass.getURI())
                .orElseThrow(() -> new IllegalArgumentException(String.format("Class <%s> not defined in the domain ontology", policyClass.getURI())));

        this.displayNameProperty = ResourceFactory.createProperty(displayNameProperty.getURI());
        this.claimValueProperty = ResourceFactory.createProperty(claimValueProperty.getURI());
        this.surfaceFormProperty = ResourceFactory.createProperty(surfaceFormProperty.getURI());

        this.parentBenefitRuleClass = getClassByUri(parentBenefitRuleClass.getURI())
                .orElseThrow(() -> new IllegalArgumentException(String.format("Class <%s> not defined in the domain ontology", parentBenefitRuleClass.getURI())));

        // using this helper method to avoid recomputing the map
        this.propertiesToBeExtractedPerBenefitRuleSubtype = cloneMapAndRefreshOntResources(propertiesToBeExtractedPerBenefitRuleSubtype);
        this.discoveredExternalEntities = new HashMap<>();
    }

    private Map<OntClass, Set<OntProperty>> cloneMapAndRefreshOntResources(Map<OntClass, Set<OntProperty>> toBeCloned) {
        return toBeCloned.entrySet().stream()
                .map(entry -> new SimpleEntry<>(refreshClass(entry.getKey()), refreshPropertiesSet(entry.getValue())))
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    private Set<OntProperty> refreshPropertiesSet(Set<OntProperty> properties) {
        // assumption: resources exist in the current model
        return properties.stream()
                .map(property -> getPropertyByUri(property.getURI()).get())
                .collect(Collectors.toSet());
    }

    private OntClass refreshClass(OntClass ontClass) {
        // assumption: resources exist in the current model
        return getClassByUri(ontClass.getURI()).get();
    }

    private void populateWithExternalEntities(Collection<ExternalEntity> externalEntities) {
        for (ExternalEntity externalEntity : externalEntities) {
            addEntityAsIndividual(externalEntity);
        }
    }

    private Individual addEntityAsIndividual(ExternalEntity externalEntity) {
        Optional<Individual> entityIndividual = addIndividual(externalEntity);
        if (!entityIndividual.isPresent()) {
            logger.warn("Individual [{}] ignored", externalEntity.getId());
            return null;
        }
        addDisplayName(entityIndividual.get(), externalEntity.getDisplayName());
        addClaimValue(entityIndividual.get(), externalEntity.getClaimValue());
        addSurfaceForms(entityIndividual.get(), externalEntity.getSurfaceForms());
        return entityIndividual.get();
    }

    public Optional<DiscoveredExternalEntity> getDiscoveredEntity(String individualUri) {
        return Optional.ofNullable(discoveredExternalEntities.get(individualUri));
    }

    private Optional<Individual> addIndividual(ExternalEntity externalEntity) {
        Optional<Individual> existentIndividual = getIndividualByUri(externalEntity.getEntityId());
        if (existentIndividual.isPresent()) {
            logger.warn("Entity with id [{}] already exists in the ontology, information will be merged!", externalEntity.getEntityId());
        }

        Optional<Individual> individual = externalEntity.getEntityTypeIds().stream()
                .map(entityTypeId -> addTypeToExistentIndividualOrCreateIt(externalEntity.getEntityId(), entityTypeId))
                .filter(Optional::isPresent)
                .reduce(StreamUtils::getLast)
                .orElse(existentIndividual);

        return individual.isPresent() ? individual : Optional.of(addIndividualWithoutType(externalEntity.getEntityId()));
    }

    private Individual addIndividualWithoutType(String entityId) {
        return model.createIndividual(entityId, OWL2.NamedIndividual);
    }

    private Optional<Individual> addTypeToExistentIndividualOrCreateIt(String entityId, String entityTypeId) {
        Optional<OntClass> entityTypeClass = getClassByUri(entityTypeId);
        if (!entityTypeClass.isPresent()) {
            logger.warn("Error while adding type to entity [{}]: class with id [{}] does not exist in the ontology", entityId, entityTypeId);
            return Optional.empty();
        }
        return Optional.of(addIndividualWithUri(entityId, entityTypeClass.get()));
    }

    private void addDisplayName(Individual individual, String displayName) {
        if (displayName != null) addStringPropertyValue(individual, displayNameProperty, displayName);
    }

    private void addClaimValue(Individual individual, String claimValue) {
        if (claimValue != null) addStringPropertyValue(individual, claimValueProperty, claimValue);
    }

    private void addSurfaceForms(Individual individual, Set<String> surfaceForms) {
        for (String surfaceForm : surfaceForms) {
            addStringPropertyValue(individual, surfaceFormProperty, surfaceForm);
        }
    }

    public Statement addStringPropertyValue(Resource resource, Property property, String value) {
        return addStatement(resource, property, value);
    }

    private Map<OntClass, Set<OntProperty>> getAllPropertiesToBeExtracted(OntClass parentBenefitRuleClass) {
        return getBenefitRuleSubtypes(parentBenefitRuleClass).stream()
                .collect(Collectors.toMap(Function.identity(), this::getAllPropertiesToBeExtractedForBenefitRuleClass));
    }

    private Set<OntProperty> getAllPropertiesToBeExtractedForBenefitRuleClass(OntClass benefitRuleClass) {
        return keepOnlyDatatypeOrObjectProperties(getAllPropertiesConnectedWithClass(benefitRuleClass));
    }

    public Set<OntClass> getBenefitRuleSubtypes(OntProperty property) {
        return propertiesToBeExtractedPerBenefitRuleSubtype.entrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(property))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private Collection<OntClass> getBenefitRuleSubtypes(OntClass parentBenefitRuleClass) {
        return getSubclasses(parentBenefitRuleClass, true, true);
    }

    private void normalise(List<NormalisationStage> normalisationStages) {
        normalisationStages.forEach(normalisationStage -> normalisationStage.apply(this.model));
    }

    private void validate() {
        // check the consistency of the model
        ValidityReport validityReport = getOntModel().validate();
        if (!validityReport.isValid()) {
            String errorReport = Streams.stream(validityReport.getReports())
                    .map(Report::toString)
                    .collect(Collectors.joining("; ", "The domain ontology is not consistent. Found issues: ", ""));
            throw new IllegalStateException(errorReport);
        }
    }

    public boolean isNegated(OntProperty property) {
        // retrieve the corresponding property in this model
        Property modelProperty = getOntModel().getProperty(property.getURI());
        Literal trueLiteral = getOntModel().createTypedLiteral(true);
        return modelProperty.hasProperty(isNegatedProperty, trueLiteral);
    }

    public Collection<String> getAffirmativeActions(OntProperty property) {
        return getOntPropertyByUri(property.getURI())
                .map(modelProperty -> getStringPropertyValues(modelProperty, affirmativeActionProperty))
                .orElse(Sets.newHashSet());
    }

    public Collection<String> getNegativeActions(OntProperty property) {
        return getOntPropertyByUri(property.getURI())
                .map(modelProperty -> getStringPropertyValues(modelProperty, negativeActionProperty))
                .orElse(Sets.newHashSet());
    }

    public Collection<String> getExtractionPatterns(OntClass ontClass) {
        return getOntResourceInThisModel(ontClass)
                .map(resourceInModel -> getStringPropertyValues(resourceInModel, extractionPatternProperty))
                .orElse(Sets.newHashSet());
    }

    public Collection<String> getAnnotatedAsValues(OntResource resource) {
        return getOntResourceInThisModel(resource)
                .map(resourceInModel -> getStringPropertyValues(resourceInModel, annotatedAsProperty))
                .orElse(Sets.newHashSet());
    }

    public Map<String, Set<Resource>> getAllResourcesPerAnnotatedAsValue() {
        return this.getStatementsWith(null, annotatedAsProperty, null).stream()
                .collect(groupingBy(statement -> statement.getObject().toString(), mapping(Statement::getSubject, Collectors.toSet())));
    }

    public Set<OntResource> getAllResourcesWithAnnotatedAs(String annotatedAsValue) {
        return this.getStatementsWith(null, annotatedAsProperty, ResourceFactory.createStringLiteral(annotatedAsValue)).stream()
                .map(Statement::getSubject)
                .map(resource -> resource.as(OntResource.class))
                .collect(Collectors.toSet());
    }


    public OntClass getPolicyClass() {
        return policyClass;
    }

    public boolean isBenefitRuleClass(OntClass ontClass) {
        return getBenefitRuleClasses().contains(ontClass);
    }

    public Collection<OntProperty> getAllPropertiesOfInterestForExtraction() {
        return propertiesToBeExtractedPerBenefitRuleSubtype.values().stream().reduce(newHashSet(), Sets::union);
    }

    public Collection<OntProperty> getAllPropertiesOfInterestForExtraction(OntClass benefitRuleSubtype) {
        return propertiesToBeExtractedPerBenefitRuleSubtype.get(benefitRuleSubtype);
    }

    public Collection<OntClass> getAllClassesOfInterestForExtraction() {
        return getBenefitRuleClasses().stream()
                .map(this::getAllClassesConnectedWithClass)
                .reduce(newHashSet(), Sets::union);
    }

    public Set<OntProperty> keepOnlyDatatypeOrObjectProperties(Set<OntProperty> properties) {
        return properties.stream()
                .filter(property -> property.canAs(DatatypeProperty.class) || property.canAs(ObjectProperty.class))
                .collect(Collectors.toSet());
    }

    /**
     * Gets all properties in the sub-graph rooted in ontClass by exploiting the domain-range relations as edges
     */
    public Set<OntProperty> getAllPropertiesConnectedWithClass(OntClass ontClass) {
        Queue<OntClass> open = new LinkedList<>();
        open.add(ontClass);
        Set<OntClass> visited = new HashSet<>();
        Set<OntProperty> properties = new HashSet<>();
        while (!open.isEmpty()) {
            OntClass currentDomain = open.poll();
            visited.add(currentDomain);
            Collection<OntProperty> propertiesWithDomain = getPropertiesWithDomain(currentDomain, false);
            Set<OntClass> newRanges = propertiesWithDomain.stream()
                    .map(property -> getSingleRangeClass(property, true))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(rangeClass -> !visited.contains(rangeClass))
                    .collect(Collectors.toSet());
            properties.addAll(propertiesWithDomain);
            open.addAll(newRanges);
        }
        return properties;
    }

    public Set<OntProperty>  getCompatiblePropertyWithRange(OntClass ontClass) {
        return getStatementsWith(null, RDFS.range, ontClass).stream()
                .map(Statement::getSubject)
                .map(resource -> resource.as(OntProperty.class))
                .collect(Collectors.toSet());
    }

    public Set<Individual> getIndividualsWithPropertyValue (OntProperty property, RDFNode value) {
        return getStatementsWith(null, property, value).stream()
                .map(Statement::getSubject)
                .map(resource -> resource.as(Individual.class))
                .collect(Collectors.toSet());
    }

    public Optional <Individual> getUniqueIndividualWithPropertyValue (OntProperty property, RDFNode value) {
        Set<Individual> individualsSet = getIndividualsWithPropertyValue(property, value);
        // if unambiguous
        if (individualsSet.size() == 1)
            return Optional.ofNullable (individualsSet.iterator().next());

        return Optional.empty();
    }

    public boolean isPropertyValueOfIndividual (Individual individual, OntProperty ontProperty, String value) {
        RDFNode valueNode = rangeNodeOf(ontProperty, value);
        Collection<Statement>  stmts = getStatementsWith(individual, ontProperty, valueNode);
        return !stmts.isEmpty();
    }


    /**
     * Gets all classes in the sub-graph rooted in ontClass by exploiting the domain-range relations as edges
     */
    public Set<OntClass> getAllClassesConnectedWithClass(OntClass ontClass) {
        Queue<OntClass> open = new LinkedList<>();
        open.add(ontClass);
        Set<OntClass> visited = new HashSet<>();
        Set<OntClass> classes = new HashSet<>();
        while (!open.isEmpty()) {
            OntClass currentDomain = open.poll();
            classes.add(currentDomain);
            visited.add(currentDomain);
            Collection<OntProperty> propertiesWithDomain = getPropertiesWithDomain(currentDomain, false);
            Set<OntClass> newRanges = propertiesWithDomain.stream()
                    .map(property -> getSingleRangeClass(property, true))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(rangeClass -> !visited.contains(rangeClass))
                    .collect(Collectors.toSet());
            open.addAll(newRanges);
        }
        return classes;
    }

    public Set<OntClass> getBenefitRuleClasses() {
        return propertiesToBeExtractedPerBenefitRuleSubtype.keySet();
    }

    public Optional<String> getDisplayName(Resource resource) {
        return getOntResourceInThisModel(resource)
                .map(thisResource -> thisResource.getPropertyValue(displayNameProperty))
                .map(label -> label.asLiteral().toString());
    }

    public String getClaimValue(Resource resource) {
        return getOntResourceInThisModel(resource)
                .map(thisResource -> thisResource.getPropertyValue(claimValueProperty))
                .map(label -> label.asLiteral().toString()).orElse(null);
    }

    // ---------------
    // SURFACE FORMS
    // ---------------

    public Collection<String> getLabels(OntResource resource) {
        return getOntResourceInThisModel(resource)
                .map(modelResource -> getStringPropertyValues(modelResource, surfaceFormProperty))
                .orElse(Sets.newHashSet());
    }

    public Set<OntResource> findAllResourcesWithLabel(String label) {
        return getOntModel().listStatements(null, surfaceFormProperty, label.toLowerCase())
                .filterKeep(statement -> statement.getSubject().canAs(OntResource.class))
                .mapWith(statement -> statement.getSubject().as(OntResource.class))
                .toSet();
    }

    public Set<Individual> findAllIndividualsWithLabel(OntClass ontClass, String label) {
        return findAllResourcesWithLabel(label.toLowerCase()).stream()
                .filter(resource -> resource.canAs(Individual.class))
                .filter(resource -> resource.hasRDFType(ontClass, false))
                .map(OntResource::asIndividual)
                .collect(Collectors.toSet());
    }

    public OntProperty findPropertyInOntology(String iri) {
        OntProperty ontProperty = getOntModel().getOntProperty(iri);
        if (ontProperty == null)
            throw new IllegalArgumentException(String.format("Property <%s> not found in the domain ontology", iri));
        return ontProperty;
    }


    public RDFNode rangeNodeOf(OntProperty property, String value) {
        if (property.isObjectProperty()) {
            return findIndividualWithUri(value);
        }
        //Find if it is an annotated datatype  (such as currency)
        else if (property.isDatatypeProperty()) {
            Collection<String> annotations = getAnnotatedAsValues(property.getRange());
            if (!annotations.isEmpty())
                return model.createTypedLiteral(value, property.getRange().getURI());
        }

        return typedLiteralOf(property.asDatatypeProperty(), value);
    }

    private Individual findIndividualWithUri(String uri) {
        return Optional.ofNullable(getOntModel().getIndividual(uri))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Individual with uri <%s> not found", uri)));
    }

    private Literal typedLiteralOf(DatatypeProperty property, String value) {
        if (Models.isIntegerDatatype(property.getRange()))
            return ResourceFactory.createTypedLiteral(Integer.valueOf(value));
        return ResourceFactory.createTypedLiteral(value);
    }

    public Property getExtractionPatternProperty() {
        return extractionPatternProperty;
    }

    // ----------------
    // DEFAULT VALUES
    // ----------------

    public Map<Resource, String> getAllPropertiesWithDefaultValues() {
        return this.getStatementsWith(null, defaultValueProperty, null).stream()
                .collect(Collectors.toMap(Statement::getSubject, statement -> getResourceUriOrValue(statement.getObject())));
    }

    public Map<Resource, Resource> getAllObjectPropertiesOfClass (OntClass ontClass ) {
        return this.getStatementsWith(ontClass, null, null).stream()
                .filter (statement -> statement.getPredicate().isProperty() &&
                        statement.getPredicate().getNameSpace().equalsIgnoreCase(this.getNamespace()) &&
                        statement.getObject().isResource())
                .collect(Collectors.toMap(Statement::getPredicate, statement -> statement.getObject().asResource()));
    }

    private String getResourceUriOrValue(RDFNode rdfNode) {
        if (rdfNode.isResource()) {
            Resource resource = rdfNode.asResource();
            return resource.getURI();
        } else if (rdfNode.isLiteral()) {
            Literal literal = rdfNode.asLiteral();
            return literal.getString();
        } else {
            throw new IllegalStateException("RDF type " + rdfNode.getClass().getTypeName() + " is not recognized.");
        }
    }

    // ----------------
    // LEAF PROPERTIES
    // ----------------

    public boolean isRootProperty(OntProperty property) {
        return hasAnyAsDomain(property, getBenefitRuleClasses());
    }

    public boolean isLeafProperty(OntProperty property) {
        return property.isDatatypeProperty() ||
                (property.isObjectProperty() && hasRangeLeafClass(property.asObjectProperty()));
    }

    private boolean hasRangeLeafClass(ObjectProperty objectProperty) {
        Optional<OntClass> singleRangeClass = getSingleRangeClass(objectProperty, true);
        return singleRangeClass.isPresent() && isLeafClass(singleRangeClass.get());
    }

    public boolean isLeafClass(OntClass ontClass) {
        if (!ontClass.isUnionClass()) return getObjectAndDataPropertiesDeclaredForClass(ontClass).isEmpty();

        // union is leaf when all its operands are leaves
        return ontClass.asUnionClass().getOperands().asJavaList().stream()
                .filter(node -> node.canAs(OntClass.class))
                .map(node -> node.as(OntClass.class))
                .allMatch(this::isLeafClass);
    }

    public Collection<OntProperty> getObjectAndDataPropertiesDeclaredForClass(OntClass ontClass) {
        return getPropertiesWithDomain(ontClass, true).stream()
                .filter(property -> property.isDatatypeProperty() || property.isObjectProperty())
                .collect(Collectors.toSet());
    }

    public Optional<? extends OntResource> getSingleRange(OntProperty property) {
        List<? extends OntResource> ranges = property.listRange().toList();
        if (ranges.size() > 1)
            throw new IllegalStateException(String.format("Too many ranges found for property %s, only one is supported (ranges: %s)", property, ranges));
        return CollectionsUtils.optionalAny(ranges);
    }

    public DomainOntology createClone() {
        return new DomainOntology(model,
                namespace,
                parentBenefitRuleClass,
                propertiesToBeExtractedPerBenefitRuleSubtype,
                displayNameProperty,
                claimValueProperty,
                surfaceFormProperty,
                policyClass,
                isNegatedProperty,
                annotatedAsProperty,
                affirmativeActionProperty,
                negativeActionProperty,
                extractionPatternProperty,
                defaultValueProperty,
                hasComplianceRuleProperty);
    }

    public Map<OntResource, Set<String>> getAllSurfaceForms() {
        return getAllAnnotationsFor(surfaceFormProperty);
    }

    public Map<OntResource, Set<String>> getAllAffirmativeActions() {
        return getAllAnnotationsFor(affirmativeActionProperty);
    }

    public Map<OntResource, Set<String>> getAllNegativeActions() {
        return getAllAnnotationsFor(negativeActionProperty);
    }

    private Map<OntResource, Set<String>> getAllAnnotationsFor(Property annotationProperty) {
        return this.getStatementsWith(null, annotationProperty, null).stream()
                .filter(statement -> statement.getSubject().canAs(OntResource.class))
                .collect(groupingBy(
                        statement -> statement.getSubject().as(OntResource.class),
                        mapping(statement -> statement.getObject().asLiteral().getString(), toSet()))
                );
    }

    public Optional<Property> getHasComplianceRuleProperty() {
        return hasComplianceRuleProperty;
    }

    public static class Builder {

        private final Collection<String> ontologyFilePaths;
        private final String ontologySyntax;
        private final String namespace;
        private String affirmativeActionPropertyUri;
        private String annotatedAsPropertyUri;
        private String defaultValuePropertyUri;
        private String hasComplianceRulePropertyUri;
        private String displayNamePropertyUri;
        private String claimValuePropertyUri;
        private String extractionPatternPropertyUri;
        private Collection<ExternalEntity> externalEntities;
        private String isNegatedPropertyUri;
        private String negativeActionPropertyUri;
        private List<NormalisationStage> normalisationStages;
        private String parentBenefitRuleClassUri;
        private String policyClassUri;
        private String surfaceFormPropertyUri;

        public Builder(
                Collection<String> ontologyFilePaths,
                String ontologySyntax,
                String namespace) {
            this.ontologyFilePaths = ontologyFilePaths;
            this.ontologySyntax = ontologySyntax;
            this.namespace = namespace;

            // default values for non-mandatory fields
            this.externalEntities = Collections.emptyList();
            this.normalisationStages = Collections.emptyList();
        }

        public Builder withExternalEntities(Collection<ExternalEntity> externalEntities) {
            this.externalEntities = externalEntities;
            return this;
        }

        public Builder withParentBenefitRuleClassUri(String parentBenefitRuleClassUri) {
            this.parentBenefitRuleClassUri = parentBenefitRuleClassUri;
            return this;
        }

        public Builder withDisplayNamePropertyUri(String displayNamePropertyUri) {
            this.displayNamePropertyUri = displayNamePropertyUri;
            return this;
        }

        public Builder withClaimValuePropertyUri(String claimValuePropertyUri) {
            this.claimValuePropertyUri = claimValuePropertyUri;
            return this;
        }

        public Builder withIsNegatedPropertyUri(String isNegatedPropertyUri) {
            this.isNegatedPropertyUri = isNegatedPropertyUri;
            return this;
        }

        public Builder withAnnotatedAsPropertyUri(String annotatedAsPropertyUri) {
            this.annotatedAsPropertyUri = annotatedAsPropertyUri;
            return this;
        }

        public Builder withAffirmativeActionPropertyUri(String affirmativeActionPropertyUri) {
            this.affirmativeActionPropertyUri = affirmativeActionPropertyUri;
            return this;
        }

        public Builder withNegativeActionPropertyUri(String negativeActionPropertyUri) {
            this.negativeActionPropertyUri = negativeActionPropertyUri;
            return this;
        }

        public Builder withSurfaceFormPropertyUri(String surfaceFormPropertyUri) {
            this.surfaceFormPropertyUri = surfaceFormPropertyUri;
            return this;
        }

        public Builder withExtractionPatternPropertyUri(String extractionPatternPropertyUri) {
            this.extractionPatternPropertyUri = extractionPatternPropertyUri;
            return this;
        }

        public Builder withDefaultValuePropertyUri(String defaultValuePropertyUri) {
            this.defaultValuePropertyUri = defaultValuePropertyUri;
            return this;
        }

        public Builder withPolicyClassUri(String policyClassUri) {
            this.policyClassUri = policyClassUri;
            return this;
        }

        public Builder withHasComplianceRulePropertyTagUri(String hasComplianceRulePropertyUri) {
            this.hasComplianceRulePropertyUri = hasComplianceRulePropertyUri;
            return this;
        }

        public Builder withNormalisationStages(List<NormalisationStage> normalisationStages) {
            this.normalisationStages = normalisationStages;
            return this;
        }

        public DomainOntology build() {

            notNullOrThrowException("affirmativeActionPropertyUri", affirmativeActionPropertyUri);
            notNullOrThrowException("annotatedAsPropertyUri", annotatedAsPropertyUri);
            notNullOrThrowException("defaultValuePropertyUri", defaultValuePropertyUri);
            notNullOrThrowException("displayNamePropertyUri", displayNamePropertyUri);
            notNullOrThrowException("claimValuePropertyUri", claimValuePropertyUri);
            notNullOrThrowException("isNegatedPropertyUri", isNegatedPropertyUri);
            notNullOrThrowException("extractionPatternPropertyUri", extractionPatternPropertyUri);
            notNullOrThrowException("negativeActionPropertyUri", negativeActionPropertyUri);
            notNullOrThrowException("parentBenefitRuleClassUri", parentBenefitRuleClassUri);
            notNullOrThrowException("policyClassUri", policyClassUri);
            notNullOrThrowException("surfaceFormPropertyUri", surfaceFormPropertyUri);
            notNullOrThrowException("hasComplianceRulePropertyUri", hasComplianceRulePropertyUri);

            return new DomainOntology(
                    ontologyFilePaths,
                    ontologySyntax,
                    namespace,
                    externalEntities,
                    parentBenefitRuleClassUri,
                    displayNamePropertyUri,
                    claimValuePropertyUri,
                    isNegatedPropertyUri,
                    annotatedAsPropertyUri,
                    affirmativeActionPropertyUri,
                    negativeActionPropertyUri,
                    surfaceFormPropertyUri,
                    extractionPatternPropertyUri,
                    defaultValuePropertyUri,
                    hasComplianceRulePropertyUri,
                    policyClassUri,
                    normalisationStages
            );

        }

        private static void notNullOrThrowException(String propertyName, String propertyUri) {
            if (propertyUri == null) {
                throw new IllegalStateException(String.format("Property [%s] is required.", propertyName));
            }
        }

    }

}
