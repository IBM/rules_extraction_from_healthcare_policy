package com.ibm.wh.extractionservice.support.jena;

import com.github.jsonldjava.core.JsonLdOptions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.jena.ext.com.google.common.base.CaseFormat;
import org.apache.jena.ontology.*;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.*;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.ibm.wh.extractionservice.commons.utils.CollectionsUtils.any;
import static com.ibm.wh.extractionservice.commons.utils.CollectionsUtils.optionalAny;
import static com.ibm.wh.extractionservice.support.NlpUtils.integerValueOf;
import static com.ibm.wh.extractionservice.support.NlpUtils.isInteger;

public class Models {

    private static final Logger logger = LoggerFactory.getLogger(Models.class);

    public static final Resource DOUBLE_DATATYPE = XSD.xdouble;
    public static final Resource INTEGER_DATATYPE = XSD.xint;
    public static final Resource STRING_DATATYPE = XSD.xstring;

    private Models() { }

    public static OntModel cloneModelRefreshingAnonIds(Model model) {
        // disclaimer: this method is a workaround, don't judge us!
        String modelAsTurtleString = toString(model, Lang.TURTLE.getLabel());
        return loadFromString(modelAsTurtleString, Lang.TURTLE);
    }

    public static OntModel loadFromString(String modelAsString, Lang syntax) {
        OntModel model = newOntModelWithoutReasoner();
        InputStream jsonLdInputStream = new ByteArrayInputStream(modelAsString.getBytes(StandardCharsets.UTF_8));
        RDFDataMgr.read(model, jsonLdInputStream, syntax);
        return model;
    }

    public static OntModel newOntModelWithoutReasoner() {
        return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
    }

    public static String toString(Model model, String syntax) {
        if (syntax.equals("JSON-LD")) return toJsonLd(model);

        StringWriter out = new StringWriter();
        model.write(out, syntax);
        String modelAsString = out.toString();

        if (syntax.equals("TURTLE")) return sortTurtle(modelAsString);
        return modelAsString;
    }

    private static String sortTurtle(String modelAsTurtleString) {
        return Arrays.stream(modelAsTurtleString.split("\n\n"))
                .map(String::trim)
                .sorted(Models::turtleStatementComparator)
                .collect(Collectors.joining("\n\n"));
    }

    private static int turtleStatementComparator(String statement1, String statement2) {
        // sort statements based on a priority value,
        // having the prefixes on top is necessary for reading the model back
        Map<String, Integer> priority = ImmutableMap.of(
                "@", 10,
                "[", 7,
                "_", 4,
                ":", 2
        );

        String firstChar1 = statement1.substring(0, 1);
        String firstChar2 = statement2.substring(0, 1);

        if (firstChar1.equals(firstChar2)) return statement1.compareTo(statement2);
        if (!priority.containsKey(firstChar1) && !priority.containsKey(firstChar2))
            return statement1.compareTo(statement2);
        if (!priority.containsKey(firstChar1)) return +1;
        if (!priority.containsKey(firstChar2)) return -1;
        return -1 * priority.get(firstChar1) + priority.get(firstChar2);
    }

    private static String toJsonLd(Model model) {
        DatasetGraph datasetGraph = DatasetFactory.wrap(model).asDatasetGraph();
        RDFWriter writer = RDFWriter.create()
                .format(RDFFormat.JSONLD)
                .context(buildJsonLdContext())
                .source(datasetGraph)
                .build();
        return writer.asString();
    }

    private static JsonLDWriteContext buildJsonLdContext() {
        JsonLDWriteContext context = new JsonLDWriteContext();
        JsonLdOptions jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setCompactArrays(false);
        context.setOptions(jsonLdOptions);
        return context;
    }

    public static Model extractSubgraphIgnoringReifiedStatements(Resource resource) {
        return SubgraphExtractor.extract(resource, false);
    }

    public static Model extractSubgraphKeepingReifiedStatements(Resource resource) {
        return SubgraphExtractor.extract(resource, true);
    }

    public static Collection<Resource> getDirectRdfTypes(Individual individual) {
        return individual.listRDFTypes(true).toSet();
    }

    public static <ResourceType extends OntResource> Collection<ResourceType> filterByNamespace(Collection<ResourceType> allResources, String namespace) {
        return allResources.stream()
                .filter(resource -> !resource.isAnon())
                .filter(resource -> namespace.equals(resource.getNameSpace()))
                .collect(Collectors.toList());
    }

    public static Property getOrCopyPropertyDefinition(Property property, OntModel destinationModel) {
        Optional<Resource> alreadyExistentResource = findResourceWithSameIdInModel(property, destinationModel);
        if (alreadyExistentResource.isPresent()) return alreadyExistentResource.get().as(Property.class);

        if (property.canAs(ObjectProperty.class))
            return copyObjectPropertyDefinition(property.as(ObjectProperty.class), destinationModel);
        if (property.canAs(DatatypeProperty.class))
            return copyDatatypePropertyDefinition(property.as(DatatypeProperty.class), destinationModel);
        if (property.canAs(AnnotationProperty.class))
            return copyAnnotationPropertyDefinition(property.as(AnnotationProperty.class), destinationModel);

        return destinationModel.createProperty(property.getURI());
    }

    private static AnnotationProperty copyAnnotationPropertyDefinition(AnnotationProperty originalProperty, OntModel destinationModel) {
        AnnotationProperty newProperty = destinationModel.createAnnotationProperty(originalProperty.getURI());
        copyDomains(originalProperty, newProperty);
        copyRanges(originalProperty, newProperty);
        return newProperty;
    }

    public static DatatypeProperty copyDatatypePropertyDefinition(DatatypeProperty originalProperty, OntModel destinationModel) {
        DatatypeProperty newProperty = destinationModel.createDatatypeProperty(originalProperty.getURI());
        copyDomains(originalProperty, newProperty);
        copyRanges(originalProperty, newProperty);
        return newProperty;
    }

    public static ObjectProperty copyObjectPropertyDefinition(ObjectProperty originalProperty, OntModel destinationModel) {
        ObjectProperty newProperty = destinationModel.createObjectProperty(originalProperty.getURI());
        copyDomains(originalProperty, newProperty);
        copyRanges(originalProperty, newProperty);
        return newProperty;
    }

    private static void copyRanges(OntProperty sourceProperty, OntProperty destinationProperty) {
        sourceProperty.listRange().forEachRemaining(range -> copyRange(range, destinationProperty));
    }

    private static void copyRange(OntResource range, OntProperty destinationProperty) {
        if (range.isDataRange() || isBuiltInDatatype(range)) {
            destinationProperty.addRange(range);
        } else if (range.isClass()) {
            destinationProperty.addRange(getOrCopyClass(range.asClass(), destinationProperty.getOntModel()));
        } else {
            logger.debug("Range <{}> has been ignored while copying the property since is not either a Class or a datatype", range);
        }
    }

    private static void copyDomains(OntProperty sourceProperty, OntProperty destinationProperty) {
        sourceProperty.listDomain()
                .filterKeep(OntResource::isClass)
                .mapWith(OntResource::asClass)
                .mapWith(ontClass -> getOrCopyClass(ontClass, destinationProperty.getOntModel()))
                .forEachRemaining(destinationProperty::addDomain);
    }

    public static OntClass getOrCopyClass(OntClass ontClass, OntModel destinationModel) {
        Optional<Resource> alreadyExistentResource = findResourceWithSameIdInModel(ontClass, destinationModel);
        if (alreadyExistentResource.isPresent()) return alreadyExistentResource.get().as(OntClass.class);

        return copyClassDefinition(ontClass, destinationModel);
    }

    public static Individual getOrCopyIndividual(Individual individual, OntModel destinationModel) {
        Optional<Resource> alreadyExistentResource = findResourceWithSameIdInModel(individual, destinationModel);
        if (alreadyExistentResource.isPresent()) return alreadyExistentResource.get().as(Individual.class);

        return deepCopyIndividual(individual, destinationModel);
    }

    /**
     * This method will make a deep copy of types, object properties and datatype properties of the individual
     *
     * @param individual
     * @param destinationModel
     * @return
     */
    private static Individual deepCopyIndividual(Individual individual, OntModel destinationModel) {
        // Anonymous ids will be the same!!
        copyIndividualSubgraph(individual, destinationModel);
        return findResourceWithSameIdInModel(individual, destinationModel)
                .orElseThrow(() -> new RuntimeException(String.format("Copy of individual <%s> failed", individual)))
                .as(Individual.class);
    }

    private static void copyIndividualSubgraph(Individual individual, OntModel destinationModel) {
        Individual newIndividual = shallowCopyIndividual(individual, destinationModel);
        deepCopyTypes(individual, newIndividual);
        deepCopyObjectPropertiesFromInto(individual, newIndividual);
        deepCopyDatatypePropertiesFromInto(individual, newIndividual);
    }

    private static void deepCopyDatatypePropertiesFromInto(Individual originalIndividual, Individual newIndividual) {
        originalIndividual.listProperties()
                .filterKeep(statement -> statement.getPredicate().canAs(DatatypeProperty.class))
                .forEachRemaining(statement -> addStatementWithPredicateAndObjectDeepCopy(newIndividual, statement.getPredicate(), statement.getObject()));
    }

    private static void deepCopyObjectPropertiesFromInto(Individual originalIndividual, Individual newIndividual) {
        originalIndividual.listProperties()
                .filterKeep(statement -> statement.getPredicate().canAs(ObjectProperty.class))
                .forEachRemaining(statement -> addStatementWithPredicateAndObjectDeepCopy(newIndividual, statement.getPredicate(), statement.getObject()));
    }

    public static Statement addStatementWithPredicateAndObjectDeepCopy(OntResource newSubject, Property property, RDFNode object) {
        // get the model form the property as there may be anonymous individuals at objects or subjects
        Property newPredicate = getOrCopyPropertyDefinition(property, newSubject.getOntModel());
        RDFNode newObject = getOrCopyRdfNode(object, newSubject.getOntModel());
        Statement newStatement = addStatement(newSubject, newPredicate, newObject);

        // IMPORTANT:
        //
        // Since we are not cloning class hierarchies when cloning a Class resource,
        // we could result having an incomplete knowledge graph, where, for example, the following statements
        // are present:
        //
        // [individual1 a ClassX]
        // [property1, rdfs:range, ClassY]
        // [otherResource property1 individual1]
        //
        // and the following statement is missing (to avoid cloning complex hierarchies in the knowledge graph):
        //
        // [ClassX rdfs:subclassOf ClassY]
        //
        // As a workaround we decided to explicitly add the statement
        //
        // [individual1 a ClassY]

        if (newObject.canAs(Individual.class)
                && newPredicate.canAs(ObjectProperty.class)
                && newPredicate.as(ObjectProperty.class).getRange() != null) {
            newSubject.getOntModel().add(newObject.as(Individual.class),
                    RDF.type,
                    newPredicate.as(ObjectProperty.class).getRange());
        }

        return newStatement;
    }

    private static RDFNode getOrCopyRdfNode(RDFNode node, OntModel destinationModel) {
        if (node.isLiteral()) return copyLiteral(node.asLiteral(), destinationModel);
        if (node.canAs(OntClass.class)) return getOrCopyClass(node.as(OntClass.class), destinationModel);
        if (node.canAs(Individual.class)) return getOrCopyIndividual(node.as(Individual.class), destinationModel);
        logger.error(String.format("The resource <%s> cannot be copied in the new model", node));
        return node;
    }

    private static Literal copyLiteral(Literal literal, OntModel destinationModel) {
        return destinationModel.createTypedLiteral(literal.getValue(), literal.getDatatypeURI());
    }

    private static void deepCopyTypes(Individual sourceIndividual, Individual destinationIndividual) {
        sourceIndividual.listRDFTypes(true).toSet().stream()
                .filter(type -> type.canAs(OntClass.class))
                .map(type -> copyClassDefinition(type.as(OntClass.class), destinationIndividual.getOntModel()))
                .forEach(copiedClass -> destinationIndividual.getOntModel().add(destinationIndividual, RDF.type, copiedClass));
    }

    private static Individual shallowCopyIndividual(Individual individual, OntModel destinationModel) {
        // the anon id will stay the same
        Resource anyDirectType = individual.getRDFType(true);
        Statement typeStatement = individual.getModel().listStatements(individual, RDF.type, anyDirectType).next();
        destinationModel.add(typeStatement);
        return findResourceWithSameIdInModel(individual, destinationModel)
                .orElseThrow(() -> new RuntimeException(String.format("The shallow copy of the individual <%s> failed", individual)))
                .as(Individual.class);
    }

    private static OntClass copyClassDefinition(OntClass ontClass, OntModel destinationModel) {
        if (ontClass.isUnionClass()) return copyUnionClass(ontClass.asUnionClass(), destinationModel);
        return destinationModel.createClass(ontClass.getURI());
    }

    private static OntClass copyUnionClass(UnionClass unionClass, OntModel destinationModel) {
        OntModel sourceModel = unionClass.getOntModel();
        RDFNode listOfClasses = sourceModel.listStatements(unionClass, OWL.unionOf, (RDFNode) null).toList().get(0).getObject();

        RDFList newList = getOrCopyRdfList(listOfClasses.as(RDFList.class), destinationModel);

        destinationModel.add(unionClass, RDF.type, OWL.Class);
        destinationModel.add(unionClass, OWL.unionOf, newList);

        return findResourceWithSameIdInModel(unionClass, destinationModel).get().as(OntClass.class);
    }

    private static RDFList getOrCopyRdfList(RDFList list, OntModel destinationModel) {
        Optional<Resource> alreadyExistentResource = findResourceWithSameIdInModel(list, destinationModel);
        if (alreadyExistentResource.isPresent())
            return alreadyExistentResource.get().as(RDFList.class);

        return copyRdfList(list, destinationModel);
    }

    private static RDFList copyRdfList(RDFList list, OntModel destinationModel) {
        Model sourceModel = list.getModel();
        RDFNode first = sourceModel.listStatements(list, RDF.first, (RDFNode) null).toList().iterator().next().getObject();
        RDFNode rest = sourceModel.listStatements(list, RDF.rest, (RDFNode) null).toList().iterator().next().getObject();

        destinationModel.add(list, RDF.first, getOrCopyClass(first.as(OntClass.class), destinationModel));

        if (rest.isURIResource() && rest.asResource().getURI().equals(RDF.nil.getURI())) {
            destinationModel.add(list, RDF.rest, RDF.nil);
            return findResourceWithSameIdInModel(list, destinationModel).get().as(RDFList.class);
        }

        RDFList copiedList = getOrCopyRdfList(rest.as(RDFList.class), destinationModel);
        destinationModel.add(list, RDF.rest, copiedList);
        return findResourceWithSameIdInModel(list, destinationModel).get().as(RDFList.class);
    }

    private static Optional<Resource> findResourceWithSameIdInModel(Resource resourceInOtherModel, OntModel model) {
        List<Statement> statements = model.listStatements(resourceInOtherModel, null, (RDFNode) null).toList();
        if (statements.isEmpty()) return Optional.empty();
        return Optional.of(statements.iterator().next().getSubject());
    }

    public static Statement addStatement(Model model, Resource r, Property p, RDFNode n) {
        Statement statement = model.createStatement(r, p, n);
        model.add(statement);
        return statement;
    }

    private static Statement addStatement(Resource r, Property p, RDFNode n) {
        Statement statement = r.getModel().createStatement(r, p, n);
        r.getModel().add(statement);
        return statement;
    }

    public static String getIdentifier(OntResource resource) {
        return resource.isAnon() ? resource.getId().toString() : resource.getURI();
    }

    public static int getStatementSubgraphHashCode(Statement statement) {
        return getStatementHashCode(statement) + getSubgraphHashCode(statement.getObject());
    }

    public static int getSubgraphHashCode(RDFNode rootNode) {
        if (rootNode.isLiteral()) return rootNode.asLiteral().getValue().toString().hashCode();
        if (rootNode.isURIResource()) return rootNode.asResource().getURI().hashCode();

        return getGraphHashCode(extractSubgraphIgnoringReifiedStatements(rootNode.asResource()).listStatements().toList());
    }

    public static int getStatementHashCode(Statement statement) {
        String urisString = "";
        urisString += statement.getSubject().isAnon() ? "AnonNode " : statement.getSubject().getURI() + ' ';
        urisString += statement.getPredicate().isAnon() ? "AnonNode " : statement.getPredicate().getURI() + ' ';

        if (statement.getObject().isLiteral())
            urisString += statement.getObject().asLiteral().getValue().toString();
        else if (statement.getObject().isAnon())
            urisString += "AnonNode";
        else
            urisString += statement.getObject().asResource().getURI();

        return urisString.hashCode();
    }

    public static int getGraphHashCode(Collection<Statement> statements) {
        return statements.stream()
                .map(Models::getStatementHashCode)
                .reduce((hash1, hash2) -> hash1 + hash2)
                .orElse(0);
    }

    public static void deepDeleteResource(Resource resourceToDelete) {
        // As first step remove all statements with as object the resource to remove and as subject
        // a resource that is not a ReifiedStatement
        deleteIncomingStatements(resourceToDelete);
        deleteOutgoingSubgraph(resourceToDelete);
    }

    private static void deleteOutgoingSubgraph(Resource resourceToDelete) {
        Model model = resourceToDelete.getModel();

        Set<Statement> statementsToDelete = extractSubgraphKeepingReifiedStatements(resourceToDelete).listStatements().toSet();
        if (statementsToDelete.isEmpty()) return;

        Set<Statement> remainingStatements = model.listStatements().toSet();
        remainingStatements.removeAll(statementsToDelete);

        // keep the definitions of the properties mentioned in the remaining model
        Set<Property> remainingProperties = remainingStatements.stream()
                .map(Statement::getPredicate)
                .collect(Collectors.toSet());

        Set<Statement> statementsToKeepForProperties = statementsToDelete.stream()
                .map(Statement::getPredicate)
                .distinct()
                .filter(remainingProperties::contains)
                // here we have the properties that are still used by someone else
                .map(property -> extractSubgraphKeepingReifiedStatements(property).listStatements().toSet())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        // keep the definitions of the resources mentioned in the remaining model
        Set<Resource> remainingObjectResources = remainingStatements.stream()
                .map(Statement::getObject)
                .filter(RDFNode::isResource)
                .map(RDFNode::asResource)
                .collect(Collectors.toSet());

        Set<Statement> statementsToKeepForObjects = statementsToDelete.stream()
                .map(Statement::getObject)
                .distinct()
                .filter(RDFNode::isResource)
                .map(RDFNode::asResource)
                .filter(remainingObjectResources::contains)
                // here we have the object resources that are still used by someone else
                .map(objectResource -> extractSubgraphKeepingReifiedStatements(objectResource).listStatements().toSet())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        statementsToDelete.removeAll(statementsToKeepForProperties);
        statementsToDelete.removeAll(statementsToKeepForObjects);

        model.remove(Lists.newArrayList(statementsToDelete));
    }

    private static void deleteIncomingStatements(Resource resourceToDelete) {
        Model model = resourceToDelete.getModel();
        List<Statement> incomingStatements = model.listStatements(null, null, resourceToDelete)
                .filterDrop(statement -> statement.getPredicate().equals(RDF.subject))
                .filterDrop(statement -> statement.getPredicate().equals(RDF.predicate))
                .filterDrop(statement -> statement.getPredicate().equals(RDF.object))
                .toList();
        incomingStatements.forEach(statement -> deleteReifiedInformation(statement));
        model.remove(incomingStatements);
    }

    private static void deleteReifiedInformation(Statement statement) {
        List<ReifiedStatement> reifiedStatements = statement.listReifiedStatements().toList();
        if (reifiedStatements.isEmpty()) return;
        reifiedStatements.forEach(reifiedStatement -> deepDeleteResource(reifiedStatement));
    }

    public static Map<Property, List<Statement>> groupByProperty(List<Statement> statements) {
        return statements.stream().collect(Collectors.groupingBy(Statement::getPredicate));
    }

    public static Map<Resource, List<Statement>> groupBySubject(List<Statement> statements) {
        return statements.stream().collect(Collectors.groupingBy(Statement::getSubject));
    }

    public static List<Statement> getStatementsWithSubject(Individual individual) {
        return individual.getModel().listStatements(individual, null, (RDFNode) null).toList();
    }

    public static boolean isSubgraph(Resource smaller, Resource bigger) {
        List<Statement> smallerStatements = smaller.getModel().listStatements().toList();
        List<Statement> biggerStatements = bigger.getModel().listStatements().toList();

        if (!Sets.difference(getAllProperties(smallerStatements), getAllProperties(biggerStatements)).isEmpty())
            // the smaller graph contains a property that is not in the bigger graph -> it's not a subgraph
            return false;

        if (!Sets.difference(getAllNonAnonRanges(smallerStatements), getAllNonAnonRanges(biggerStatements)).isEmpty())
            // the smaller graph contains a non-anon node that is not in the bigger graph -> it's not a subgraph
            return false;

        Set<Statement> smallerIndividualStatements = getStatementsWithSubject(smallerStatements, smaller);

        for (Statement smallerStatement : smallerIndividualStatements) {
            RDFNode smallerObject = smallerStatement.getObject();

            if (!smallerObject.isAnon()) {
                // the object is not anon, if the bigger doesn't contain it return false, otherwise go ahead
                if (statementExists(bigger.getModel(), bigger, smallerStatement.getPredicate(), smallerStatement.getObject()))
                    continue;
                else return false;
            }

            List<Statement> biggerStatementsWithSameProperty = bigger.getModel().listStatements(bigger, smallerStatement.getPredicate(), (RDFNode) null).toList();
            if (biggerStatementsWithSameProperty.size() > 1) {
                logger.error("There is more than one statement with property " + smallerStatement.getPredicate() + " and anonymous range, we don't know which one is matching!");
                return false;
            }

            if (!isSubgraph(smallerObject.asResource(), any(biggerStatementsWithSameProperty).getObject().asResource()))
                return false;
        }

        // if it didn't stop before...yay!
        return true;
    }

    public static void removeIndividual(Individual individual) {
        deepDeleteResource(individual);
    }

    private static boolean statementExists(Model model, Resource subject, Property predicate, RDFNode object) {
        return model.listStatements(subject, predicate, object).hasNext();
    }

    private static Set<Statement> getStatementsWithSubject(List<Statement> statements, Resource subject) {
        return statements.stream().filter(statement -> statement.getSubject().equals(subject)).collect(Collectors.toSet());
    }

    private static Set<Property> getAllProperties(List<Statement> statements) {
        return statements.stream().map(Statement::getPredicate).collect(Collectors.toSet());
    }

    private static Set<RDFNode> getAllNonAnonRanges(List<Statement> statements) {
        return statements.stream().map(Statement::getObject).filter(node -> !node.isAnon()).collect(Collectors.toSet());
    }

    public static Individual addAnonymousIndividual(OntModel model, OntClass ontClass) {
        OntClass classInModel = getOrCopyClass(ontClass, model);
        return model.createIndividual(classInModel);
    }

    public static ReifiedStatement getOrCreateReifiedStatement(OntModel model, Statement statement) {
        return model.getAnyReifiedStatement(statement)
                .as(ReifiedStatement.class);
    }

    public static Collection<Individual> getIndividuals(OntClass ontClass, boolean onlyDirect) {
        return ontClass.listInstances(onlyDirect).mapWith(OntResource::asIndividual).toSet();
    }

    public static void cleanEmptyReifiedStatement(Statement statement) {
        ReifiedStatement reifiedStatement = getOrCreateReifiedStatement((OntModel) statement.getModel(), statement);
        if (hasNoProperty(reifiedStatement)) {
            statement.removeReification();
        }
    }

    private static boolean hasNoProperty(ReifiedStatement reifiedStatement) {
        return !reifiedStatement.listProperties()
                .filterDrop(statement -> isPartOfReifiedStatementDefinition(statement))
                .hasNext();
    }

    private static boolean isPartOfReifiedStatementDefinition(Statement statement) {
        return statement.getPredicate().equals(RDF.type) ||
                statement.getPredicate().equals(RDF.subject) ||
                statement.getPredicate().equals(RDF.predicate) ||
                statement.getPredicate().equals(RDF.object);
    }

    //
    // DATA TYPES HELPERS
    //

    public static boolean isValueCompatibleWithDatatype(String text, OntResource datatype) {
        if (isIntegerDatatype(datatype)) {
            return isInteger(text.trim());
        } else return isStringDatatype(datatype);
    }

    public static boolean isDatatype(OntResource resource) {
        return isUserDefinedDatatype(resource) || isBuiltInDatatype(resource);
    }

    public static boolean isUserDefinedDatatype(OntResource datatype) {
        return datatype.hasProperty(RDF.type, RDFS.Datatype);
    }

    public static boolean isBuiltInDatatype(OntResource resource) {
        return isBuiltInIntegerDatatype(resource) || isBuiltInStringDatatype(resource) || isBuiltInDoubleDatatype(resource);
    }

    public static boolean isUserDefinedDatatypeBasedOn(OntResource datatype, Resource baseDatatype) {
        return getBaseDatatype(datatype)
                .map(resource -> resource.equals(baseDatatype))
                .orElse(false);
    }

    public static boolean isStringDatatype(OntResource datatype) {
        return isBuiltInStringDatatype(datatype) ||
                isUserDefinedDatatypeBasedOn(datatype, STRING_DATATYPE);
    }

    public static boolean isBuiltInStringDatatype(OntResource datatype) {
        return datatype.getURI().equalsIgnoreCase(STRING_DATATYPE.getURI());
    }

    public static boolean isBuiltInDoubleDatatype(OntResource datatype) {
        return datatype.getURI().equalsIgnoreCase(DOUBLE_DATATYPE.getURI());
    }

    public static boolean isIntegerDatatype(OntResource datatype) {
        return isBuiltInIntegerDatatype(datatype) ||
                isUserDefinedDatatypeBasedOn(datatype, INTEGER_DATATYPE);
    }

    public static boolean isBuiltInIntegerDatatype(OntResource datatype) {
        return datatype.getURI().equalsIgnoreCase(INTEGER_DATATYPE.getURI());
    }

    public static Optional<Resource> getBaseDatatype(OntResource userDefinedDatatype) {
        if (!userDefinedDatatype.isClass()) return Optional.empty();

        // get base datatypes from the equivalent classes definition (it should be max one)
        Set<Resource> baseDatatypesFromEquivalentClasses = userDefinedDatatype.asClass().listEquivalentClasses().toSet()
                .stream()
                .map(Models::getBaseDatatype)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        getDirectBaseDatatype(userDefinedDatatype)
                .ifPresent(baseDatatypesFromEquivalentClasses::add);

        if (baseDatatypesFromEquivalentClasses.size() > 1)
            throw new IllegalStateException("More than one different base datatype available for user-defined datatype " + userDefinedDatatype.getURI());

        return optionalAny(baseDatatypesFromEquivalentClasses);
    }

    private static Optional<Resource> getDirectBaseDatatype(OntResource ontResource) {
        if (!ontResource.isClass()) return Optional.empty();
        return ontResource.listPropertyValues(OWL2.onDatatype)
                .mapWith(RDFNode::asResource)
                .nextOptional();
    }

    public static Literal createMatchingTypedLiteral(DatatypeProperty property, String text) throws NumberFormatException {
        if (isIntegerDatatype(property.getRange())) {
            return ResourceFactory.createTypedLiteral(integerValueOf(text.trim()));
        } else {
            return ResourceFactory.createTypedLiteral(text);
        }
    }

    public static <Type extends OntResource> SortedSet<Type> sortByUri(Collection<Type> resources) {
        TreeSet<Type> objects = new TreeSet<>(Comparator.comparing(Models::resourceId).reversed());
        objects.addAll(resources);
        return objects;
    }

    private static String resourceId(OntResource resource) {
        return resource.isAnon() ? resource.getId().toString() : resource.getURI();
    }

    public static String getLocalName(String uri) {
        return uri.substring(Util.splitNamespaceXML(uri));
    }

    public static String generateUriForIndividual(String namespace, String individualTypeUri, String individualName) {
        String cleanedIndividualName = (getIndividualNamePrefix(individualTypeUri) + toSnakeCase(individualName))
                .replaceAll("[^a-zA-Z\\d\\s:]", "_");
        return namespace + cleanedIndividualName;
    }

    private static String getIndividualNamePrefix(String individualTypeUri) {
        // if the type uri is `http://some.prefix/FancyIndividualType`
        // the returned prefix will be `fancy_individual_type_`
        String results = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getLocalName(individualTypeUri));
        return results + "_";
    }

    private static String toSnakeCase(String name) {
        return name.trim()
                .toLowerCase()
                .replaceAll(" ", "_");
    }

    public static Property property(String propertyUri) {
        return ResourceFactory.createProperty(propertyUri);
    }

}
