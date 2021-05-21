package com.ibm.wh.extractionservice.support.jena;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.jena.graph.Triple;
import org.apache.jena.graph.TripleBoundary;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StatementBoundary;
import org.apache.jena.rdf.model.StatementTripleBoundary;
import org.apache.jena.vocabulary.RDF;

public class SubgraphExtractor {

    /**
     * A statement boundary with no limit
     */
    public static final StatementBoundary STOP_NOWHERE_BOUNDARY = new StatementTripleBoundary(TripleBoundary.stopNowhere);

    /**
     * A statement boundary that stops at reified statements and do not follow subject, predicate, object properties
     * of the reified statement
     */
    public static final StatementBoundary STOP_AT_REIFIED_STATEMENT_SOURCE = new StatementTripleBoundary(
            triple -> triple.getPredicate().getURI().equals(RDF.subject.getURI())
                    || triple.getPredicate().getURI().equals(RDF.predicate.getURI())
                    || triple.getPredicate().getURI().equals(RDF.object.getURI()));

    private SubgraphExtractor() { }

    public static StatementBoundary statementBoundary(Predicate<Triple> stopWhen) {
        return new StatementTripleBoundary(stopWhen::test);
    }

    public static Model extract(Model model, Resource rootResource, boolean includeReified) {
        return extract(model, rootResource, STOP_AT_REIFIED_STATEMENT_SOURCE, includeReified);
    }

    public static Model extract(Model model, Resource rootResource, StatementBoundary boundary, boolean includeReified) {
        Model newModel = ModelFactory.createDefaultModel();
        Queue<Resource> open = new LinkedList<>();
        open.add(rootResource);

        Set<String> visited = new HashSet<>();
        visited.add(getIdentifier(rootResource));

        while (!open.isEmpty()) {
            Resource current = open.poll();
            List<Statement> statements = model.listStatements(current, null, (RDFNode) null).toList();

            for (Statement statement : statements) {
                newModel.add(statement);

                if (includeReified && statement.isReified()) {
                    // add all reified statements as resources to be processed
                    model.listReifiedStatements(statement).toList()
                            .stream()
                            .filter(res -> !visited.contains(getIdentifier(res)))
                            .forEachOrdered(res -> {
                                visited.add(getIdentifier(res));
                                open.add(res);
                            });
                }

                if (!boundary.stopAt(statement)) {
                    // visit also properties to include the definition of the properties
                    if (statement.getPredicate().isResource()) {
                        Resource predicateResource = statement.getPredicate().asResource();
                        if (!visited.contains(getIdentifier(predicateResource))) {
                            visited.add(getIdentifier(predicateResource));
                            open.add(predicateResource);
                        }
                    }

                    if (statement.getObject().isResource()) {
                        Resource objectResource = statement.getObject().asResource();
                        if (!visited.contains(getIdentifier(objectResource))) {
                            visited.add(getIdentifier(objectResource));
                            open.add(objectResource);
                        }
                    }
                }
            }

        }

        return newModel;
    }

    private static String getIdentifier(Resource resource) {
        return resource.isAnon() ? resource.getId().toString() : resource.getURI();
    }

    public static Model extract(Resource rootResource, boolean includeReified) {
        return extract(rootResource.getModel(), rootResource, STOP_AT_REIFIED_STATEMENT_SOURCE, includeReified);
    }

    public static Model extract(Resource rootResource, StatementBoundary boundary, boolean includeReified) {
        return extract(rootResource.getModel(), rootResource, STOP_AT_REIFIED_STATEMENT_SOURCE, includeReified);
    }

}
