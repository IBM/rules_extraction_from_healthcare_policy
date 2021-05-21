package com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate;

import java.util.Objects;

import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = FreeTextConditionTemplate.class, name = AbstractConditionTemplate.Type.FREE_TEXT_AS_STRING),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = SingleSelectionConditionTemplate.class, name = AbstractConditionTemplate.Type.SINGLE_SELECTION_AS_STRING),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = MultipleSelectionConditionTemplate.class, name = AbstractConditionTemplate.Type.MULTIPLE_SELECTION_AS_STRING),
})
public abstract class AbstractConditionTemplate {

    @Id
    private String id;
    private String displayName;
    private Type type;

    protected AbstractConditionTemplate() {
        // required by Jackson!
    }

    protected AbstractConditionTemplate(String id, String label, Type type) {
        this.id = id;
        this.displayName = label;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractConditionTemplate that = (AbstractConditionTemplate) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(displayName, that.displayName) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, type);
    }

    @Override
    public String toString() {
        return "AbstractConditionTemplate{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", type=" + type +
                '}';
    }

    public enum Type {

        FREE_TEXT,
        SINGLE_SELECTION,
        MULTIPLE_SELECTION;

        public static final String FREE_TEXT_AS_STRING = "FREE_TEXT";
        public static final String SINGLE_SELECTION_AS_STRING = "SINGLE_SELECTION";
        public static final String MULTIPLE_SELECTION_AS_STRING = "MULTIPLE_SELECTION";

    }

}
