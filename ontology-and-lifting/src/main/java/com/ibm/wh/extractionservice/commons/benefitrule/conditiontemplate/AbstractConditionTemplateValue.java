package com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.AbstractConditionTemplate.Type;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "conditionTemplateType", visible = true)
@JsonSubTypes({
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = FreeTextConditionTemplateValue.class, name = AbstractConditionTemplate.Type.FREE_TEXT_AS_STRING),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = SingleSelectionConditionTemplateValue.class, name = AbstractConditionTemplate.Type.SINGLE_SELECTION_AS_STRING),
        @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = MultipleSelectionConditionTemplateValue.class, name = AbstractConditionTemplate.Type.MULTIPLE_SELECTION_AS_STRING),
})
public abstract class AbstractConditionTemplateValue {

    private String conditionTemplateId;
    private Type conditionTemplateType;
    private String displayName;

    protected AbstractConditionTemplateValue() {
        // required by Jackson!
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractConditionTemplateValue that = (AbstractConditionTemplateValue) o;
        return Objects.equals(conditionTemplateId, that.conditionTemplateId) &&
                conditionTemplateType == that.conditionTemplateType &&
                displayName.equals(that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditionTemplateId, conditionTemplateType, displayName);
    }

    @Override
    public String toString() {
        return "AbstractConditionTemplateValue{" + "conditionTemplateId='" + conditionTemplateId + '\'' + ", conditionTemplateType=" + conditionTemplateType  + ", displayName=" + displayName + '}';
    }

    @JsonIgnore
    public abstract boolean containsValue(Object value);

}
