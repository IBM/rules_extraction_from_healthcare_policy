package com.ibm.wh.extractionservice.support.jena;

import static com.google.common.collect.Sets.*;
import static com.ibm.wh.extractionservice.commons.utils.CollectionsUtils.*;
import static com.ibm.wh.extractionservice.support.jena.Models.*;
import static java.util.Collections.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ReifiedStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.ibm.wh.extractionservice.ontology.Syntax;

public class OntModelAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OntModelAdapter.class);

    protected final String namespace;
    protected final boolean isRdfsInferenceEnabled;
    protected OntModel model;

    protected OntModelAdapter(String baseNamespace, boolean enableRdfsInference) {
        namespace = baseNamespace;
        model = newOwlOntModel(baseNamespace, enableRdfsInference);
        isRdfsInferenceEnabled = enableRdfsInference;
    }

    protected OntModelAdapter(Collection<String> ontologyFilePaths, String ontologyFileSyntax, String baseNamespace, boolean enableRdfsInference) {
        this(baseNamespace, enableRdfsInference);
        model = newOwlOntModel(namespace, isRdfsInferenceEnabled);
        ontologyFilePaths.forEach(ontologyFile -> model.read(Paths.get(ontologyFile).toUri().toString(), ontologyFileSyntax));
    }

    protected OntModelAdapter(OntModel model, String baseNamespace, boolean enableRdfsInference) {
        this(baseNamespace, enableRdfsInference);
        this.model.add(model);
    }

    protected OntModel newOwlOntModel(String namespace, boolean enableRdfsInference) {

        // IMPORTANT:

        // OWL_MEM_RDFS_INF reasoner supports inference, for example, over sub and super type relations, typeof, etc.
        // OWL_*_RULE_INF reasoners support inference related to union of classes, stronger domain / range validation,
        // disjointness constraints (only full and mini) and cardinality constraints (only with values 0 and 1).
        // Memo: if we decide to enable OWL_*_RULE_INF we need at least to revise all the getDomain / getRange function
        // calls, because the reasoner seems to explicitly add all superclasses of the declared domain / range
        // as domain / range.
        //
        // Refer to https://jena.apache.org/documentation/inference/#rdfs for details about the
        // inference capabilities of each reasoner.

        OntModelSpec ontModelSpec = enableRdfsInference ? OntModelSpec.OWL_MEM_RDFS_INF : OntModelSpec.OWL_MEM;

        return (OntModel) ModelFactory.createOntologyModel(ontModelSpec)
                .setNsPrefix("", namespace);
    }

    protected void replaceModel(OntModel model) {
        this.model = model;
    }

    protected OntModel cloneOntModel() {
        OntModel ontModel = newOwlOntModel(namespace, isRdfsInferenceEnabled);
        ontModel.add(model);
        return ontModel;
    }

    public String getNamespace() {
        return namespace;
    }

    public Optional<OntClass> getClassByName(String name) {
        return getClassByUri(namespace + name);
    }

    public Optional<OntClass> getClassByUri(String classUri) {
        return Optional.ofNullable(model.getOntClass(classUri));
    }

    public Optional<Individual> getIndividualByName(String name) {
        return getIndividualByUri(namespace + name);
    }

    public Optional<Individual> getIndividualByUri(String individualUri) {
        return Optional.ofNullable(model.getIndividual(individualUri));
    }

    public Collection<OntClass> getClasses(boolean onlyInNamespace) {
        Set<OntClass> allClasses = model.listClasses().toSet();
        if (onlyInNamespace) return filterByNamespace(allClasses, namespace);
        return allClasses;
    }

    public Collection<OntClass> getSubclasses(OntClass ontClass, boolean onlyDirect, boolean onlyInNamespace) {
        Collection<OntClass> allSubClasses = getClassByUri(ontClass.getURI()) // get the class in this model
                .map(modelClass -> modelClass.listSubClasses(onlyDirect).toSet())
                .orElse(newHashSet());
        if (onlyInNamespace) return filterByNamespace(allSubClasses, namespace);
        return allSubClasses;
    }

    public boolean isSubClassOf(OntClass type, OntClass supertype) {
        return type.hasProperty(RDFS.subClassOf, supertype);
    }

    public Collection<OntClass> getAllDirectTypes(Resource resource, boolean onlyInNamespace) {
        Set<OntClass> allTypes = model.listObjectsOfProperty(resource, RDF.type)
                .filterKeep(node -> node.canAs(OntClass.class))
                .mapWith(node -> node.as(OntClass.class))
                .toSet();

        if (onlyInNamespace) return filterByNamespace(allTypes, namespace);
        return allTypes;
    }

    public Collection<OntClass> getAllTypes(Resource resource, boolean onlyInNamespace, boolean direct) {
        if (direct)
            // Use the specific and more efficient method
            return getAllDirectTypes(resource, onlyInNamespace);

        Optional<Resource> resourceInThisModel = getResourceInThisModel(resource);
        if (!resourceInThisModel.isPresent()) return emptyList();

        Set<OntClass> allTypes = resourceInThisModel.get().as(OntResource.class)
                .listRDFTypes(false)
                .filterKeep(type -> type.canAs(OntClass.class))
                .mapWith(type -> type.as(OntClass.class))
                .toSet();

        if (onlyInNamespace) return filterByNamespace(allTypes, namespace);
        return allTypes;
    }

    public Collection<OntClass> getAllTypes(String resourceUri, boolean onlyInNamespace, boolean direct) {
        Optional<Individual> individual = getIndividualByUri(resourceUri);
        if (!individual.isPresent())
            return new HashSet<>();
        return getAllTypes(individual.get(), onlyInNamespace, direct);
    }

    public Collection<Individual> getIndividuals(OntClass ontClass, boolean onlyDirect) {
        return getClassByUri(ontClass.getURI())
                .map(modelClass -> Models.getIndividuals(modelClass, onlyDirect))
                .orElse(emptySet());
    }

    public Optional<OntProperty> getPropertyByName(String name) {
        return getOntPropertyByUri(namespace + name);
    }

    public Optional<OntProperty> getPropertyByUri(String uri) {
        return getOntPropertyByUri(uri);
    }

    protected Optional<OntProperty> getOntPropertyByUri(String propertyUri) {
        return Optional.ofNullable(model.getOntProperty(propertyUri));
    }

    public Collection<ObjectProperty> getObjectProperties(boolean onlyInNamespace) {
        Set<ObjectProperty> allObjectProperties = model.listObjectProperties().toSet();

        if (onlyInNamespace) return filterByNamespace(allObjectProperties, namespace);
        return allObjectProperties;
    }

    public Collection<DatatypeProperty> getDatatypeProperties(boolean onlyInNamespace) {
        Set<DatatypeProperty> allDatatypeProperties = model.listDatatypeProperties().toSet();

        if (onlyInNamespace) return filterByNamespace(allDatatypeProperties, namespace);
        return allDatatypeProperties;
    }

    public Collection<OntProperty> getPropertiesWithDomain(OntClass domain, boolean onlyInNamespace) {
        // get all types, i.e. domain + superclasses(domain), consider also if the domain is part of a union
        Collection<OntClass> domains = newHashSet(domain);
        domains.addAll(getSuperclasses(domain, false, false));

        Stream<OntProperty> ontPropertyStream = model.listOntProperties().toSet().stream()
                .filter(ontProperty -> hasAnyAsDomain(ontProperty, domains));

        if (onlyInNamespace)
            ontPropertyStream = ontPropertyStream.filter(ontProperty -> namespace.equalsIgnoreCase(ontProperty.getNameSpace()));

        return ontPropertyStream.collect(Collectors.toSet());
    }

    protected boolean hasAnyAsDomain(OntProperty ontProperty, Collection<OntClass> domains) {
        return domains.stream().anyMatch(domain -> hasAsDomain(ontProperty, domain));
    }

    /**
     * Checks if `domain` is a valid domain class for `property`, this is true if
     * - `domain` is listed as a direct domain for `property` - !! this is inaccurate
     * because in case of multiple domains we should check the intersection (currently not supported)
     * - the domain of `property` is a union of classes and `domain` is a direct operand of this union
     *
     * @param property
     * @param domain
     * @return
     */
    public boolean hasAsDomain(OntProperty property, OntClass domain) {
        return property.listDomain()
                .filterKeep(resource -> resource.canAs(OntClass.class))
                .mapWith(resource -> resource.as(OntClass.class))
                .toSet().stream()
                .anyMatch(propertyDomain -> propertyDomain.equals(domain) || isPartOfUnion(domain, propertyDomain));
    }

    protected boolean isPartOfUnion(OntClass part, OntClass union) {
        if (part.equals(union)) return true;
        return union.canAs(UnionClass.class) && isPartOfUnion(part, union.asUnionClass());
    }

    protected boolean isPartOfUnion(OntClass part, UnionClass union) {
        return union.hasOperand(part);
    }

    public Collection<OntProperty> getPropertiesWithDomainAndRange(OntClass domain, Resource range) {
        Collection<OntProperty> propertiesWithRange = getPropertiesWithRange(range, false);
        Collection<OntProperty> propertiesWithDomain = getPropertiesWithDomain(domain, false);
        propertiesWithRange.retainAll(propertiesWithDomain);
        return propertiesWithRange;
    }

    public Collection<DatatypeProperty> getDatatypePropertiesWithDomainAndRange(OntClass domain, Resource range) {
        return getPropertiesWithDomainAndRange(domain, range).stream()
                .filter(OntProperty::isDatatypeProperty)
                .map(OntProperty::asDatatypeProperty)
                .collect(Collectors.toSet());
    }

    public Collection<ObjectProperty> getObjectPropertiesWithDomainAndRange(OntClass domain, OntClass range) {
        return getPropertiesWithDomainAndRange(domain, range).stream()
                .filter(OntProperty::isObjectProperty)
                .map(OntProperty::asObjectProperty)
                .collect(Collectors.toSet());
    }

    public Collection<ObjectProperty> getObjectPropertiesWithRange(OntClass range, boolean onlyInNamespace) {
        return getPropertiesWithRange(range, onlyInNamespace).stream()
                .filter(OntResource::isObjectProperty)
                .map(OntResource::asObjectProperty)
                .collect(Collectors.toSet());
    }

    public Collection<OntProperty> getPropertiesWithRange(Resource range, boolean onlyInNamespace) {
        // get all types, i.e. range + superclasses(range), if is a class
        Set<Resource> ranges = new HashSet<>();
        ranges.add(range);
        if (range.canAs(OntClass.class))
            ranges.addAll(getSuperclasses(range.as(OntClass.class), false, false));

        Stream<OntProperty> ontPropertyStream = ranges.stream()
                .map(type -> model.listSubjectsWithProperty(RDFS.range, type))
                .map(ExtendedIterator::toSet)
                .flatMap(Collection::stream)
                .map(resource -> resource.as(OntProperty.class))
                // filter out properties without a domain
                .filter(ontProperty -> ontProperty.getDomain() != null);

        if (onlyInNamespace)
            ontPropertyStream = ontPropertyStream.filter(ontProperty -> namespace.equalsIgnoreCase(ontProperty.getNameSpace()));

        return ontPropertyStream.collect(Collectors.toSet());
    }

	  public Collection<OntResource> getAllSuperclasses(Resource value) {

		if (value.isAnon()) return emptySet();

	  	Set<OntResource> allSuperclasses = getOntResourceInThisModel(value)
	          .map(modelResource -> getObjectPropertyValues(modelResource, RDFS.subClassOf))
	          .orElse(Sets.newHashSet());

	  	return filterByNamespace(allSuperclasses, namespace);
	}


    public Collection<OntClass> getSuperclasses(OntClass ontClass, boolean onlyDirect, boolean onlyInNamespace) {
        if (ontClass.isAnon()) return emptySet();

        // FIXME Vale: listSuperClasses is really slow 
        Collection<OntClass> allSuperclasses = getClassByUri(ontClass.getURI())
                .map(modelOntClass -> modelOntClass.listSuperClasses(onlyDirect).toSet())
                .orElse(newHashSet());
        if (onlyInNamespace) return filterByNamespace(allSuperclasses, namespace);

        return allSuperclasses;
    }

    public Collection<DatatypeProperty> getIndividualDatatypePropertiesWithRangeType(Individual individual, Resource range) {
        Set<DatatypeProperty> individualProperties = individual.listProperties()
                .mapWith(Statement::getPredicate)
                .filterKeep(property -> property.canAs(DatatypeProperty.class))
                .mapWith(property -> property.as(DatatypeProperty.class))
                .toSet();

        Collection<OntProperty> propertiesWithRange = getPropertiesWithRange(range, false);
        individualProperties.retainAll(propertiesWithRange);
        return individualProperties;
    }

    public Collection<Statement> getIndividualObjectAndDatatypeProperties(Individual individual, boolean onlyInNamespace) {
        ExtendedIterator<Statement> statements = individual.listProperties()
                .filterKeep(statement -> statement.getPredicate().canAs(ObjectProperty.class)
                        || statement.getPredicate().canAs(DatatypeProperty.class));

        if (onlyInNamespace)
            statements = statements.filterKeep(statement -> namespace.equalsIgnoreCase(statement.getPredicate().getNameSpace()));

        return statements.toSet();
    }

    public Collection<Statement> getIndividualProperties(Resource resource, boolean onlyPropertiesInNamespace) {
        ExtendedIterator<Statement> statements = resource.listProperties();

        if (onlyPropertiesInNamespace)
            statements = statements.filterKeep(statement -> statement.getPredicate().getNameSpace().equalsIgnoreCase(namespace));

        return statements.toList();
    }

    public OntModelAdapter extractSubgraph(Resource resource) {
        OntModel newModel = (OntModel) newOwlOntModel(namespace, false)
                .add(SubgraphExtractor.extract(model, resource, true));
        return new OntModelAdapter(newModel, namespace, false);
    }

    public <T extends OntModelAdapter> void extractSubgraph(Resource resource, T outputOntModel) {
        outputOntModel.add(SubgraphExtractor.extract(model, resource, true));
    }

    protected void add(Model other) {
        model.add(other);
    }

    /**
     * Return a collection of Statements with the specified subject, predicate and object.
     * The null value acts as a wild card.
     *
     * @param subject
     * @param predicate
     * @param object
     * @return
     */
    public Collection<Statement> getStatementsWith(Resource subject, Property predicate, RDFNode object) {
        return model.listStatements(subject, predicate, object).toList();
    }

    public boolean isIsomorphicWith(OntModelAdapter other) {
        return model.isIsomorphicWith(other.model);
    }

    public Individual addAnonymousIndividualWithIdentifier(OntClass ontClass, String identifier) {
        logger.debug("Add anonymous individual with identifier '{}'", identifier);
        Resource resource = model.createResource(new AnonId(identifier));
        OntClass newClass = getOrCopyClass(ontClass, model);
        resource.addProperty(RDF.type, newClass);

        return resource.as(Individual.class);
    }

    public Individual addAnonymousIndividual(OntClass ontClass) {
        logger.debug("Add anonymous individual of type {}", ontClass.getLocalName());
        return Models.addAnonymousIndividual(model, ontClass);
    }

    public Individual getOrCreateAnonymousIndividualById(String identifier) {
        return model.createResource(new AnonId(identifier)).as(Individual.class);
    }

    public void addSubtypeToIndividual(Individual individual, OntClass ontClass) {
        logger.debug("Add subtype {} to individual {}", ontClass.getLocalName(), getIdentifier(individual));
        OntClass newClass = getOrCopyClass(ontClass, model);

        //model.createIndividual(individual.getURI(), newClass);        
        Individual indiv = getOrCopyIndividual(individual, model);//individual.getOntModel());
        Statement statement = model.createStatement(indiv, RDF.type, newClass);
        // addStatementWithPredicateAndObjectDeepCopy(indiv, RDF.type, newClass); // to copy the relations if an instance such as adult

        model.add(statement);

    }

    protected Individual addIndividualWithUri(String uri, OntClass ontClass) {
        // add the individual and its class
        //logger.debug("Add individual: {} of type {}", uri, ontClass.getLocalName());
        OntClass newClass = getOrCopyClass(ontClass, model);
        return model.createIndividual(uri, newClass);
    }

    public Individual addIndividualWithLocalName(String individualName, OntClass ontClass) {
        return addIndividualWithUri(namespace + individualName, ontClass);
    }

    public Individual addIndividualWithLabel(String individualName, OntClass ontClass) {
        String[] words = individualName.split(" ");
        for (int i = 0; i < words.length; i++)
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
        String localName = String.join(" ", words).replaceAll(" ", "");

        Individual newindiv = addIndividualWithUri(namespace + localName, ontClass);
        newindiv.addLabel(model.createLiteral(individualName));
        return newindiv;
    }

    public Individual addUmlsIndividualWithLabel(String umlsType, String individualName, OntClass ontClass) {
        String[] words = individualName.split(" ");
        for (int i = 0; i < words.length; i++)
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
        String localName = String.join(" ", words).replaceAll(" ", "");
        localName = umlsType + "_" + localName;

        Individual newindiv = addIndividualWithUri(namespace + localName, ontClass);
        newindiv.addLabel(model.createLiteral(individualName));
        return newindiv;
    }

    public Statement addDatatypePropertyValue(Individual domainIndividual, DatatypeProperty datatypeProperty, Literal rangeValue) {
        logger.debug("Add data property value: {} - {} - {}", getIdentifier(domainIndividual), datatypeProperty.getLocalName(), rangeValue);

        Property datatypePropertyInModel = getOrCopyPropertyDefinition(datatypeProperty, model);
        return addStatement(domainIndividual, datatypePropertyInModel, rangeValue);
    }

    public Statement addDatatypePropertyValue(Statement statement, DatatypeProperty datatypeProperty, Literal rangeValue) {
        logger.debug("Add data property value: {} - {} - {}", statement, datatypeProperty.getLocalName(), rangeValue);

        Resource reifiedStatement = getOrCreateReifiedStatement(statement);
        Property datatypePropertyInModel = getOrCopyPropertyDefinition(datatypeProperty, model);
        return addStatement(reifiedStatement, datatypePropertyInModel, rangeValue);
    }

    /**
     * Return a single domain class if present, (optionally) filtering by namespace.
     * If the filter by namespace is enabled, it ignores anonymous classes as well.
     *
     * @param property
     * @param onlyInNamespace
     * @return
     */
    public Optional<OntClass> getSingleDomainClass(OntProperty property, boolean onlyInNamespace) {
        ExtendedIterator<OntClass> domains = property.listDomain()
                .filterKeep(OntResource::isClass)
                .mapWith(OntResource::asClass);

        if (onlyInNamespace)
            domains = domains
                    .filterDrop(resource -> resource.isAnon())
                    .filterKeep(resource -> resource.getNameSpace().equalsIgnoreCase(namespace));

        Set<OntClass> domainsSet = domains.toSet();
        if (domainsSet.size() > 1)
            logger.error("Multiple domain classes available for property {}: {}", property, domainsSet);

        return domainsSet.isEmpty() ? Optional.empty() : Optional.of(domainsSet.iterator().next());
    }

    // it fails for hasExcludedService (union of domains)
    public Collection<OntClass> getAllDomainClass(OntProperty property, boolean onlyInNamespace) {
        ExtendedIterator<OntClass> domains = property.listDomain()
                .filterKeep(OntResource::isClass)
                .mapWith(OntResource::asClass);

        if (onlyInNamespace)
            domains = domains
                    .filterDrop(resource -> resource.isAnon())
                    .filterKeep(resource -> resource.getNameSpace().equalsIgnoreCase(namespace));

        Set<OntClass> domainsSet = domains.toSet();
        if (domainsSet.size() > 1)
            logger.error("Multiple domain classes available for property {}: {}", property, domainsSet);

        return domainsSet;
    }

    public Collection<OntClass> getUnionOfDomainClass(OntProperty property, boolean onlyInNamespace) {

        OntClass domain = property.getDomain().asClass();
        if (!domain.isUnionClass())
            return getAllDomainClass (property, onlyInNamespace);

        ExtendedIterator<OntClass> domains = domain.asUnionClass().listOperands()
                .filterKeep(OntResource::isClass)
                .mapWith(OntResource::asClass);

        if (onlyInNamespace) {
            domains = domains
                    .filterDrop(resource -> resource.isAnon())
                    .filterKeep(resource -> resource.getNameSpace().equalsIgnoreCase(namespace));
        }

        return domains.toSet();
    }

    protected static Statement addStatement(Model model, Resource r, Property p, RDFNode n) {
        return Models.addStatement(model, r, p, n);
    }

    public Statement addStatement(Resource r, Property p, RDFNode n) {
        return addStatement(model, r, p, n);
    }

    public ReifiedStatement getOrCreateReifiedStatement(Statement statement) {
        return Models.getOrCreateReifiedStatement(model, statement);
    }

    public OntModel getOntModel() {
        return model;
    }

    public Statement addObjectPropertyValue(Individual domainIndividual, ObjectProperty objectProperty, Individual rangeIndividual) {
        logger.debug("Add object property value: {} - {} - {}", getIdentifier(domainIndividual), objectProperty.getLocalName(), getIdentifier(rangeIndividual));
        return addStatementWithPredicateAndObjectDeepCopy(domainIndividual, objectProperty, rangeIndividual);

    }

    public Optional<OntClass> getSingleRangeClass(OntProperty property, boolean onlyInNamespace) {
        ExtendedIterator<OntClass> ranges = property.listRange()
                .filterKeep(OntResource::isClass)
                .mapWith(OntResource::asClass);

        if (onlyInNamespace)
            ranges = ranges.filterKeep(resource -> resource.getNameSpace().equalsIgnoreCase(namespace));

        Set<OntClass> rangesSet = ranges.toSet();
        if (rangesSet.size() > 1)
            logger.error("Multiple range classes available for property {}: {}", property, rangesSet);

        return rangesSet.isEmpty() ? Optional.empty() : Optional.of(rangesSet.iterator().next());
    }

    public Statement addAnnotationPropertyValue(Statement statement, AnnotationProperty annotationProperty, Individual individual) {
        return addAnnotationPropertyValue(getOrCreateReifiedStatement(statement), annotationProperty, individual);
    }

    public Statement addAnnotationPropertyValue(Resource resource, AnnotationProperty annotationProperty, Individual individual) {
        return addStatement(resource, annotationProperty, individual);
    }

    public Statement addAnnotationPropertyValue(Statement statement, AnnotationProperty annotationProperty, Literal literal) {
        return addAnnotationPropertyValue(getOrCreateReifiedStatement(statement), annotationProperty, literal);
    }

    public Statement addAnnotationPropertyValue(Resource resource, AnnotationProperty annotationProperty, Literal literal) {
        return addStatement(resource, annotationProperty, literal);
    }

    public Statement addStatement(Resource r, Property p, String s) {
        return Models.addStatement(model, r, p, ResourceFactory.createStringLiteral(s));
    }

    public void add(OntModelAdapter other) {
        model.add(other.model);
    }

    /**
     * Remove the individual and the subgraph rooted in it, including the
     * corresponding reified information
     *
     * @param individual
     */
    public void removeIndividual(Individual individual) {
        getOntResourceInThisModel(individual)
                .ifPresent(Models::deepDeleteResource);
    }

    /**
     * Remove the resource and the subgraph rooted in it, including the
     * corresponding reified information
     *
     * @param resource
     */
    public void removeResource(Resource resource) {
        getResourceInThisModel(resource)
                .ifPresent(Models::deepDeleteResource);
    }

    /**
     * Remove the statement and the reified information from the model.
     * If the object resource is not used by any other statement in the model
     * it will be removed as well.
     *
     * @param statement
     */
    public void removeStatement(Statement statement) {
        List<Statement> matchingStatement = model.listStatements(statement.getSubject(), statement.getPredicate(), statement.getObject()).toList();
        if (matchingStatement.isEmpty()) return;
        Statement statementInModel = any(matchingStatement);

        statementInModel.listReifiedStatements().forEachRemaining(this::removeResource);
        model.remove(statementInModel);

        if (statementInModel.getObject().isResource() && isNotUsed(statementInModel.getObject()))
            removeResource(statementInModel.getObject().asResource());
    }

    private boolean isNotUsed(RDFNode node) {
        return !node.getModel().listStatements(null, null, node).hasNext();
    }

    private Collection<Statement> getStatementsWithObject(RDFNode objectNode) {
        return model.listStatements(null, null, objectNode).toSet();
    }

    private Collection<Statement> getStatementsWithSubject(Resource subjectNode) {
        return model.listStatements(subjectNode, null, (RDFNode) null).toSet();
    }

    public OntClass createClass(String uri) {
        return model.createClass(uri);
    }

    public DatatypeProperty createDatatypeProperty(String uri) {
        return model.createDatatypeProperty(uri);
    }

    public ObjectProperty createObjectProperty(String uri) {
        return model.createObjectProperty(uri);
    }

    public AnnotationProperty createAnnotationProperty(String uri) {
        return model.createAnnotationProperty(uri);
    }

    public void write(Path outputFile, String syntax) {
        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            model.write(writer, syntax);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write the model to file", e);
        }
    }

    public void write(OutputStream outputStream, String syntax) {
        model.write(outputStream, syntax);
    }

    @Override
    public String toString() {
        return model.listStatements().toList().stream()
                .sorted(Comparator.comparing(Object::toString))
                .map(Statement::toString)
                .collect(Collectors.joining("\n"));
    }

    public String toString(Syntax syntax) {
        return Models.toString(model, syntax.getName());
    }

    private boolean hasRdfType(OntResource resource, String typeUri) {
        return getOntResourceInThisModel(resource)
                .map(ontResource -> ontResource.hasRDFType(typeUri))
                .orElse(false);
    }
    
    public Optional<OntResource> getOntResourceInThisModel(String resourceUri) {
   	 	return Optional.ofNullable ( model.getOntResource(resourceUri));
   }
    
    protected Optional<OntResource> getOntResourceInThisModel(Resource resource) {
        return getResourceInThisModel(resource)
                .map(res -> res.as(OntResource.class));
    }

    protected Optional<Resource> getResourceInThisModel(Resource resource) {
        // to retrieve an anonymous resource is necessary to use the method createResource
        if (resource.isAnon())
            return Optional.ofNullable(model.createResource(new AnonId(resource.getId().toString())).as(OntResource.class));
        else return Optional.ofNullable(model.getOntResource(resource.getURI()));
    }

    protected Collection<String> getStringPropertyValues(OntResource resource, Property property) {
        // assumption: resource and property belongs to the model
        // the property range is a literal
        return resource.listPropertyValues(property)
                .filterKeep(RDFNode::isLiteral)
                .mapWith(literal -> literal.asLiteral().getString())
                .toSet();
    }

    protected Set<OntResource> getObjectPropertyValues(OntResource resource, Property property) {
        // assumption: resource and property belongs to the model
        // the property range is an object   	
        return resource.listPropertyValues(property)
                .filterKeep(RDFNode::isResource)
                .mapWith(res -> res.asResource().as(OntResource.class))
                .filterKeep(res -> namespace.equals(res.getNameSpace()))
                .toSet();
    }

    public void removeAllAnnotationProperties() {
        model.listAnnotationProperties().toList()
                .forEach(this::cleanFromAnnotationProperty);
    }

    private void cleanFromAnnotationProperty(AnnotationProperty annotationProperty) {
        // remove usages
        removeAllStatements(null, annotationProperty, null);
        // remove definition
        removeAllStatements(annotationProperty, null, null);
    }

    public void removeAllStatements(Resource r, Property p, RDFNode n) {
        model.removeAll(r, p, n);
    }

    public void removeReifiedStatementWithoutProperties(Statement statement) {
        if (!hasSomeProperty(getOrCreateReifiedStatement(statement), true)) {
            // if the reified statement does not have any property remove it
            statement.removeReification();
        }
    }

    public boolean hasSomeProperty(ReifiedStatement reifiedStatement, boolean onlyPropertiesInNamespace) {
        return resourceHasSomeProperty(reifiedStatement, onlyPropertiesInNamespace);
    }

    public boolean hasSomeProperty(Individual individual, boolean onlyPropertiesInNamespace) {
        return resourceHasSomeProperty(individual, onlyPropertiesInNamespace);
    }

    private boolean resourceHasSomeProperty(Resource resource, boolean onlyPropertiesInNamespace) {
        ExtendedIterator<Statement> statements = resource.listProperties();

        if (onlyPropertiesInNamespace)
            statements = statements.filterKeep(statement -> statement.getPredicate().getNameSpace().equalsIgnoreCase(namespace));

        return statements.hasNext();
    }

    public Set<Individual> deleteEmptyAnonIndividual(Individual individual) {
        Optional<Individual> parentIndividual = getParentIndividualInCurrentGraph(individual);
        removeIndividual(individual);

        Set<Individual> deletedParentIndividuals = parentIndividual.filter(this::isEmptyAnonIndividual)
                .map(this::deleteEmptyAnonIndividual)
                .orElse(emptySet());

        return Sets.union(Collections.singleton(individual), deletedParentIndividuals);
    }

    private Optional<Individual> getParentIndividualInCurrentGraph(Individual individual) {
        // NOTE: the parent individual can be only one because of the constraints on the ontology
        Set<Individual> parentNodes = getStatementsWith(null, null, individual).stream()
                .filter(statement -> statement.getPredicate().getNameSpace().equalsIgnoreCase(getNamespace()))
                .map(Statement::getSubject)
                .filter(resource -> resource.canAs(Individual.class))
                .map(resource -> resource.as(Individual.class))
                .collect(Collectors.toSet());

        if (parentNodes.size() > 1)
            throw new IllegalStateException("More than one parent node found for anonymous individual " + individual);

        return parentNodes.isEmpty() ? Optional.empty() : Optional.of(parentNodes.iterator().next());
    }

    public boolean isEmptyAnonIndividual(Individual individual) {
        return individual.isAnon() && !hasSomeProperty(individual, true);
    }

    public Set<Individual> deleteAllEmptyAnonIndividuals() {
        Set<Individual> deletedIndividuals = new HashSet<>();
        Set<Individual> anonIndividuals = getAllEmptyAnonIndividuals();
        for (Individual anonIndiv : anonIndividuals) {
            deletedIndividuals.addAll(deleteEmptyAnonIndividual(anonIndiv));
        }
        return deletedIndividuals;
    }

    public Set<Individual> getAllEmptyAnonIndividuals() {
        return getStatementsWith(null, RDF.type, null).stream()
                // this filter keeps only anon individuals that have a type that is anonymous or in our namespace
                // needed to avoid discarding union class definitions (type: Class)
                .filter(statement -> statement.getObject().isURIResource() && statement.getObject().asResource().getNameSpace().equalsIgnoreCase(namespace))
                .map(Statement::getSubject)
                .filter(resource -> resource.canAs(Individual.class))
                .map(resource -> resource.as(Individual.class))
                .filter(resource -> isEmptyAnonIndividual(resource.as(Individual.class)))
                .collect(Collectors.toSet());
    }

    public Map<OntClass, Set<Individual>> getAllAnonIndividualsPerClass() {
        return this.model.listClasses()
                .filterKeep(this::isInNamespace)
                .toSet().stream().distinct()
                .collect(Collectors.toMap(Function.identity(), this::getAllAnonIndividuals));
    }

    private Set<Individual> getAllAnonIndividuals(OntClass ontClass) {
        return getIndividuals(ontClass, true).stream()
                .filter(Individual::isAnon)
                .collect(Collectors.toSet());
    }

    public Collection<Individual> getAllIndividuals() {
        return this.model.listClasses()
                .filterKeep(this::isInNamespace)
                .toSet().stream().distinct()
                .map(ontClass -> getIndividuals(ontClass, true))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private boolean isInNamespace(Resource resource) {
        return namespace.equals(resource.getNameSpace());
    }

    public boolean isIndividual(String resourceUri) {
        // We can not use the method RDFNode.canAs(Class<T>) because it's
        // returning true for Individual also when the uri resource is a class or a datatype
        Optional<Resource> resourceInThisModel = getResourceInThisModel(ResourceFactory.createResource(resourceUri));
        if (!resourceInThisModel.isPresent() || !resourceInThisModel.get().canAs(OntResource.class)) return false;
        return !getAllDirectTypes(resourceInThisModel.get().as(OntResource.class), true).isEmpty();
    }

    public boolean isDatatype(String resourceUri) {
        Optional<Resource> resourceInThisModel = getResourceInThisModel(ResourceFactory.createResource(resourceUri));
        if (!resourceInThisModel.isPresent() || !resourceInThisModel.get().canAs(OntResource.class)) return false;
        return Models.isDatatype(resourceInThisModel.get().as(OntResource.class));
    }

    protected String getBaseNamespaceFromOntologyReference(String ontologyReference) throws URISyntaxException {
        return new URI(ontologyReference).getHost();
    }

}