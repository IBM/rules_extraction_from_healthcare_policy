package com.ibm.wh.extractionservice.externalentity.lifting.support;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.CustomPropertyMapping;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.CustomPropertyMapping.CustomProperty;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.PredefinedPropertyMapping;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.PredefinedPropertyMapping.PredefinedProperty;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.PropertyMapping;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.PropertyMapping.RangeType;
import com.ibm.wh.extractionservice.externalentity.lifting.LiftingConfiguration.PropertyMapping.Transformation;

public class PropertyMappingDeserializer extends StdDeserializer<PropertyMapping> {

    public PropertyMappingDeserializer(Class<?> clazz) {
        super(clazz);
    }

    @Override
    public PropertyMapping deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        JsonNode property = node.get("property");

        List<String> columns = getList(node, "columns");
        String formatter = getOrNull(node, "formatter");
        String transformationAsString = getOrNull(node, "transformation");
        Transformation transformation = transformationAsString == null ? null : Transformation.valueOf(transformationAsString);

        if (property != null && property.isTextual()) {
            PredefinedProperty propertyValue = PredefinedProperty.valueOf(property.asText());
            return new PredefinedPropertyMapping(columns, formatter, transformation, propertyValue);
        } else if (property != null && property.isObject()) {
            String uri = getOrNull(property, "uri");
            RangeType type = RangeType.valueOf(getOrNull(property, "type"));
            String typeUri = getOrNull(property, "typeUri");
            return new CustomPropertyMapping(columns, formatter, transformation, new CustomProperty(uri, type, typeUri));
        }

        throw new IOException("Cannot construct instance of `LiftingRequest$PropertyMapping`, " +
                "required property [property] with value of type [LiftingRequest$PredefinedProperty] " +
                "or [LiftingRequest$CustomPropertyMapping`");
    }

    private List<String> getList(JsonNode node, String field) {
        if (node.get(field) == null) return Collections.emptyList();
        if (!node.get(field).isArray()) return Collections.emptyList();
        return StreamSupport.stream(node.get(field).spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());
    }

    private String getOrNull(JsonNode node, String field) {
        if (node.get(field) == null) return null;
        return node.get(field).asText();
    }

}
