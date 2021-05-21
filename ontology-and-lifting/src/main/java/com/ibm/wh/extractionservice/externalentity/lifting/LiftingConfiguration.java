package com.ibm.wh.extractionservice.externalentity.lifting;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.wh.extractionservice.externalentity.lifting.support.PropertyMappingDeserializer;
import org.apache.commons.lang3.StringUtils;

public class LiftingConfiguration {

    private String namespace;
    private Collection<ColumnMapping> columns;

    public LiftingConfiguration() {
        // Required by Jackson!
    }

    public String getNamespace() {
        return namespace;
    }

    public Collection<ColumnMapping> getColumns() {
        return columns;
    }

    public static class ColumnMapping {

        public enum EntityType {
            INDIVIDUAL, GROUP
        }

        private String column;
        private String type;
        private EntityType entityType;
        private final Collection<PropertyMapping> properties;

        public ColumnMapping() {
            // Required by Jackson!
            properties = Collections.emptyList();
        }

        public String getColumn() {
            return column;
        }

        public String getType() {
            return type;
        }

        public EntityType getEntityType() {
            return entityType == null ? EntityType.INDIVIDUAL : entityType;
        }

        public Collection<PropertyMapping> getProperties() {
            return properties;
        }

    }

    @JsonDeserialize(using = PropertyMappingDeserializer.class)
    public abstract static class PropertyMapping {

        private final List<String> columns;
        private final String formatter;
        private final Transformation transformation;

        protected PropertyMapping(List<String> columns, String formatter, Transformation transformation) {
            this.columns = columns;
            this.formatter = formatter;
            this.transformation = transformation;
        }

        protected PropertyMapping(List<String> columns) {
            this(columns, null, defaultCaseTransformation());
        }

        public List<String> getColumns() {
            return columns;
        }

        public String getFormatter() {
            return formatter == null ? defaultFormatter() : formatter;
        }

        public Transformation getTransformation() {
            return transformation;
        }

        private String defaultFormatter() {
            // Default style: <column1> - <column2> - <column3>
            if (columns == null) return "%s";
            return columns.stream()
                    .map(rangeValue -> "%s")
                    .collect(Collectors.joining(" - "));
        }

        private static Transformation defaultCaseTransformation() {
            return Transformation.NONE;
        }

        public enum RangeType {

            LITERAL, INDIVIDUAL

        }

        public enum Transformation {

            UPPERCASE, LOWERCASE, SWAPCASE, CAPITALIZE, UNCAPITALIZE, NONE;

            public String apply(String text) {
                switch (this) {
                    case UPPERCASE:
                        return StringUtils.upperCase(text);
                    case LOWERCASE:
                        return StringUtils.lowerCase(text);
                    case SWAPCASE:
                        return StringUtils.swapCase(text);
                    case CAPITALIZE:
                        return StringUtils.capitalize(text);
                    case UNCAPITALIZE:
                        return StringUtils.uncapitalize(text);
                    case NONE:
                        return text;
                    default:
                        throw new IllegalStateException(String.format("Case transformation %s not supported", this));
                }
            }

        }

    }

    public static class PredefinedPropertyMapping extends PropertyMapping {

        private PredefinedProperty property;

        public PredefinedPropertyMapping(List<String> columns, String formatter, Transformation transformation, PredefinedProperty property) {
            super(columns, formatter, transformation);
            this.property = property;
        }

        private PredefinedPropertyMapping() {
            // Required by Jackson!
            super(Collections.emptyList());
        }

        public enum PredefinedProperty {

            DISPLAY_NAME, SURFACE_FORM, DESCRIPTION, CLAIM_VALUE

        }

        public PredefinedProperty getProperty() {
            return property;
        }

    }

    public static class CustomPropertyMapping extends PropertyMapping {

        private CustomProperty property;

        public CustomPropertyMapping() {
            // Required by Jackson!
            super(Collections.emptyList());
        }

        public CustomPropertyMapping(List<String> columns, String formatter, Transformation transformation, CustomProperty property) {
            super(columns, formatter, transformation);
            this.property = property;
        }

        public CustomProperty getProperty() {
            return property;
        }

        public static class CustomProperty {

            private String uri;
            private RangeType type;
            private String typeUri;

            public CustomProperty() {
                // Required by Jackson!
            }

            private static RangeType defaultRangeType() {
                return RangeType.LITERAL;
            }

            public CustomProperty(String uri, RangeType type, String typeUri) {
                this.uri = uri;
                this.type = type;
                this.typeUri = typeUri;
            }

            public String getUri() {
                return uri;
            }

            public RangeType getType() {
                return type == null ? defaultRangeType() : type;
            }

            public String getTypeUri() {
                return typeUri;
            }

        }

    }

}
