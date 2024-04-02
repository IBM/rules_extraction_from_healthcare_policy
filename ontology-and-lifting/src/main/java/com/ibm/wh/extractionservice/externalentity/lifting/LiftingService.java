package com.ibm.wh.extractionservice.externalentity.lifting;

import static com.ibm.wh.extractionservice.support.jena.Models.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity;
import com.ibm.wh.extractionservice.commons.externalentity.type.GroupExternalEntity;
import com.ibm.wh.extractionservice.commons.externalentity.type.IndividualExternalEntity;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.ColumnMapping;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.ColumnMapping.EntityType;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.CustomPropertyMapping;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.PredefinedPropertyMapping;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.PredefinedPropertyMapping.PredefinedProperty;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.PropertyMapping;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.PropertyMapping.Transformation;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

@Service
public class LiftingService {

    public Collection<ExternalEntity> liftEntities(InputStream dataAsCsvStream, String entitiesTag, LiftingConfiguration configuration) {
        try (TabularDataReader tabularDataReader = new CsvTabularDataReader(dataAsCsvStream)) {
            ExternalEntitiesParser externalEntitiesParser = new ExternalEntitiesParser(tabularDataReader, configuration.getNamespace(), configuration.getColumns(), entitiesTag);
            return externalEntitiesParser.parse();
        } catch (IOException e) {
            throw new RuntimeException("Something bad happened when lifting external entities from csv", e);
        }
    }

    private interface TabularDataReader extends AutoCloseable {

        boolean hasCurrentLine();

        void moveToNextLine();

        Optional<String> getValue(String column);

        void close() throws IOException;

    }

    private static class CsvTabularDataReader implements TabularDataReader {

        private static final Logger logger = LoggerFactory.getLogger(CsvTabularDataReader.class);

        private final CSVReader csvReader;
        private Map<String, Integer> columnHeadersToPosition;
        private String[] currentLine;

        public CsvTabularDataReader(String dataAsCsv) {
            this.csvReader = new CSVReader(new StringReader(dataAsCsv));
            initReader();
        }

        public CsvTabularDataReader(InputStream dataAsCsvStream) {
            this.csvReader = new CSVReader(new InputStreamReader(dataAsCsvStream));
            initReader();
        }

        private void initReader() {
            try {
                this.currentLine = csvReader.readNext();
                this.columnHeadersToPosition = cleanHeaders(currentLine);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (CsvValidationException e){
                throw new RuntimeException("A CsvValidationException occurred", e);
            }
        }

        @Override
        public boolean hasCurrentLine() {
            return currentLine != null;
        }

        @Override
        public void moveToNextLine() {
            try {
                this.currentLine = csvReader.readNext();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (CsvValidationException e){
                throw new RuntimeException("A CsvValidationException occurred", e);
            }
        }

        @Override
        public Optional<String> getValue(String column) {
            Optional<Integer> columnIndex = getColumnIndex(column);
            return columnIndex.map(index -> currentLine[index]);
        }

        @Override
        public void close() throws IOException {
            this.csvReader.close();
        }

        private static Map<String, Integer> cleanHeaders(String[] csvHeaderLine) {
            Map<String, Integer> headerToPosition = new HashMap<>();
            for (int i = 0; i < csvHeaderLine.length; i++) {
                headerToPosition.put(cleanForComparison(csvHeaderLine[i]), i);
            }
            return headerToPosition;
        }

        private static String cleanForComparison(String input) {
            return input.toLowerCase().replace('\uFEFF', ' ').trim();
        }

        private Optional<Integer> getColumnIndex(String column) {
            String cleanedColumn = cleanForComparison(column);
            try{
                int index = columnHeadersToPosition.get(cleanedColumn);
                if (index == -1) {
                    logger.error("Column with name [{}] not found in {}", cleanedColumn, columnHeadersToPosition.keySet());
                    return Optional.empty();

                }
                return Optional.of(index);
            } catch(Exception e){
                logger.error("Column with name [{}] not found in {}", cleanedColumn, columnHeadersToPosition.keySet());
                return Optional.empty();
            }
        }

    }

    private static class ExternalEntitiesParser {

        private final TabularDataReader tabularDataReader;
        // Namespace need to end with `/` or `#`
        private final String namespace;
        private final Collection<ColumnMapping> columnMappings;
        private final String entitiesTag;

        public ExternalEntitiesParser(TabularDataReader tabularDataReader, String namespace, Collection<ColumnMapping> columnMappings) {
            this(tabularDataReader, namespace, columnMappings, null);
        }

        public ExternalEntitiesParser(TabularDataReader tabularDataReader, String namespace, Collection<ColumnMapping> columnMappings, String entitiesTag) {
            this.tabularDataReader = tabularDataReader;
            this.namespace = normaliseNamespace(namespace);
            this.columnMappings = columnMappings;
            this.entitiesTag = entitiesTag;
        }

        private String normaliseNamespace(String namespace) {
            namespace = namespace.trim();
            if (namespace.endsWith("/") || namespace.endsWith("#")) return namespace;
            return namespace + "/";
        }

        public Collection<ExternalEntity> parse() {
            Map<String, ExternalEntity> entities = new HashMap<>();
            tabularDataReader.moveToNextLine();
            while (tabularDataReader.hasCurrentLine()) {
                // if an entity already exists we'll merge the information, if possible
                for (ExternalEntity externalEntity : parseEntitiesFromCurrentLine()) {
                    if (!entities.containsKey(externalEntity.getEntityId()))
                        entities.put(externalEntity.getEntityId(), externalEntity);
                    else
                        entities.put(externalEntity.getEntityId(), entities.get(externalEntity.getEntityId()).merge(externalEntity));
                }
                tabularDataReader.moveToNextLine();
            }
            return entities.values();
        }

        private List<ExternalEntity> parseEntitiesFromCurrentLine() {
            // each column mapping corresponds to a new entity
            return columnMappings.stream()
                    .map(mapping -> new ExternalEntityParser(mapping, entitiesTag).parse())
                    .collect(Collectors.toList());
        }

        private class ExternalEntityParser {

            private final ColumnMapping mapping;
            private final String entityTag;
            private String entityId;
            private String description;
            private String displayName;
            private String claimValue;
            private final Set<String> surfaceForms;
            private Set<String> entityTypeIds;

            public ExternalEntityParser(ColumnMapping mapping, String entityTag) {
                this.mapping = mapping;
                this.entityTag = entityTag;
                this.surfaceForms = new HashSet<>();
                this.entityTypeIds = new HashSet<>();
            }

            public ExternalEntity parse() {
                // Each parse* method populates some of the instance fields
                parseEntityId();
                parseEntityTypes();
                parseProperties();
                if (entityId.isEmpty()) throw new IllegalStateException("Entity id cannot be empty");
                if (mapping.getEntityType() == EntityType.GROUP) {
                    return new GroupExternalEntity(entityId, entityTypeIds, displayName, surfaceForms, description, entityTag, claimValue, Collections.emptySet());
                } else if (mapping.getEntityType() == EntityType.INDIVIDUAL) {
                    return new IndividualExternalEntity(entityId, entityTypeIds, displayName, surfaceForms, description, entityTag, claimValue);
                } else {
                    throw new IllegalArgumentException(String.format("EntityType value [%s] is not currently supported", mapping.getEntityType()));
                }
            }

            private void parseEntityId() {
                Optional<String> columnValue = tabularDataReader.getValue(mapping.getColumn());
                if (!columnValue.isPresent())
                    throw new IllegalArgumentException(String.format("Error while reading value for column [%s]", mapping.getColumn()));
                String typeUri = mapping.getType();
                this.entityId = buildEntityId(typeUri, columnValue.get());
            }

            private void parseEntityTypes() {
                this.entityTypeIds = Sets.newHashSet(mapping.getType());
            }

            private void parseProperties() {
                for (PropertyMapping propertyMapping : mapping.getProperties()) {
                    parseProperty(propertyMapping);
                }
            }

            private void parseProperty(PropertyMapping propertyMapping) {
                if (propertyMapping instanceof PredefinedPropertyMapping) {
                    parsePredefinedProperty((PredefinedPropertyMapping) propertyMapping);
                } else if (propertyMapping instanceof CustomPropertyMapping) {
                    parseCustomProperty((CustomPropertyMapping) propertyMapping);
                } else {
                    throw new IllegalArgumentException(String.format("This should not happen! Property mapping type [%s] not supported", propertyMapping.getClass()));
                }
            }

            private void parsePredefinedProperty(PredefinedPropertyMapping propertyMapping) {
                switch (propertyMapping.getProperty()) {
                    case DISPLAY_NAME:
                        this.displayName = readValueAsString(propertyMapping);
                        break;
                    case SURFACE_FORM:
                        this.surfaceForms.add(readValueAsString(propertyMapping));
                        break;
                    case DESCRIPTION:
                        this.description = readValueAsString(propertyMapping);
                        break;
                    case CLAIM_VALUE:
                        this.claimValue = readValueAsString(propertyMapping);
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("This should not happen! Predefined property [%s] not found", propertyMapping.getProperty()));
                }
            }

            private String readValueAsString(PropertyMapping propertyMapping) {
                return String.format(propertyMapping.getFormatter(), (Object[]) getColumnValues(propertyMapping));
            }

            private String[] getColumnValues(PropertyMapping propertyMapping) {
                return propertyMapping.getColumns().stream()
                        .map(column -> tabularDataReader.getValue(column))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(value -> applyCaseTransformation(propertyMapping.getTransformation(), value))
                        .collect(Collectors.toList())
                        .toArray(new String[]{});
            }

            private String applyCaseTransformation(Transformation transformation, String columnValues) {
                return transformation.apply(columnValues);
            }

            private void parseCustomProperty(CustomPropertyMapping propertyMapping) {
                throw new UnsupportedOperationException(String.format("Custom properties definitions are currently not supported! Do you want to use any of the predefined properties %s?", Arrays.toString(PredefinedProperty.values())));
            }

            private String buildEntityId(String typeUri, String value) {
                return generateUriForIndividual(namespace, typeUri, value);
            }

        }

    }

}
