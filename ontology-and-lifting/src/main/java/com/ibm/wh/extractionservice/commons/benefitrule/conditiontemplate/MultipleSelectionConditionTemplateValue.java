package com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate;

import java.util.Objects;
import java.util.Set;

import org.springframework.data.annotation.TypeAlias;

import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.value.CandidateValue;

@TypeAlias("MultipleSelectionConditionTemplateValue")
public class MultipleSelectionConditionTemplateValue extends AbstractConditionTemplateValue {

    private Set<CandidateValue> values;

    @SuppressWarnings("unused")
    private MultipleSelectionConditionTemplateValue() {
        // required by Jackson!
    }

    public Set<CandidateValue> getValues() {
        return values;
    }

    @Override
    public boolean containsValue(Object thatValue) {
        if (!(thatValue instanceof CandidateValue)) {
            throw new IllegalArgumentException(String.format("Expected argument value of class CandidateValue, found %s", thatValue.getClass().getName()));
        }
        return this.values.contains(thatValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MultipleSelectionConditionTemplateValue that = (MultipleSelectionConditionTemplateValue) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), values);
    }

    @Override
    public String toString() {
        return "MultipleSelectionConditionTemplateValue{" +
                super.toString() +
                ", values=" + values +
                '}';
    }

}
