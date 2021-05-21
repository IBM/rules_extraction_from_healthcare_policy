package com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate;

import java.util.Objects;

import org.springframework.data.annotation.TypeAlias;

@TypeAlias("FreeTextConditionTemplateValue")
public class FreeTextConditionTemplateValue extends AbstractConditionTemplateValue {

    private String value;

    @SuppressWarnings("unused")
    public FreeTextConditionTemplateValue() {
        // Required by Jackson!
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean containsValue(Object thatValue) {
        if (!(thatValue instanceof String)) {
            throw new IllegalArgumentException(String.format("Expected argument value of class String, found %s", thatValue.getClass().getName()));
        }
        return this.value.equals(thatValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FreeTextConditionTemplateValue that = (FreeTextConditionTemplateValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public String toString() {
        return "FreeTextConditionTemplateValue{" +
                super.toString() +
                ", value='" + value + '\'' +
                '}';
    }

}
