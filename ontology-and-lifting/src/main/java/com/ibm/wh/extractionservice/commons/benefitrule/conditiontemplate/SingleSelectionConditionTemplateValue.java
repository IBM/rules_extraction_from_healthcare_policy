package com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate;

import java.util.Objects;

import org.springframework.data.annotation.TypeAlias;

import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.value.CandidateValue;

@TypeAlias("SingleSelectionConditionTemplateValue")
public class SingleSelectionConditionTemplateValue extends AbstractConditionTemplateValue {

    private CandidateValue value;

    @SuppressWarnings("unused")
    public SingleSelectionConditionTemplateValue() {
        // Required by Jackson!
    }

    public CandidateValue getValue() {
        return value;
    }

    @Override
    public boolean containsValue(Object thatValue) {
        if (!(thatValue instanceof CandidateValue)) {
            throw new IllegalArgumentException(String.format("Expected argument value of class CandidateValue, found %s", thatValue.getClass().getName()));
        }
        return this.value.equals(thatValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SingleSelectionConditionTemplateValue that = (SingleSelectionConditionTemplateValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @Override
    public String toString() {
        return "SingleSelectionConditionTemplateValue{" +
                super.toString() +
                ", value=" + value +
                '}';
    }

}
