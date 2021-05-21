package com.ibm.wh.extractionservice.conditiontemplate.defaultvalue;

import java.util.Objects;

public class ConditionTemplateDefaultValue {

    private String conditionTemplateId;
    // Currently, the value is either a String value, e.g. "20" or an candidate value id,
    // e.g. "http://claims-audit.ibm.com/procedure_code_97001"
    private String value;

    public ConditionTemplateDefaultValue() {
        // required by Jackson
    }

    public ConditionTemplateDefaultValue(String conditionTemplateId, String value) {
        this.conditionTemplateId = conditionTemplateId;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionTemplateDefaultValue that = (ConditionTemplateDefaultValue) o;
        return Objects.equals(conditionTemplateId, that.conditionTemplateId) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditionTemplateId, value);
    }
}
