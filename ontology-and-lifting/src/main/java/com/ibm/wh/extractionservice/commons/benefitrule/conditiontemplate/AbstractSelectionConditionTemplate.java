package com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate;

import java.util.Objects;
import java.util.Set;

import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.value.CandidateValue;

public abstract class AbstractSelectionConditionTemplate extends AbstractConditionTemplate {

    private Set<CandidateValue> candidateValues;

    protected AbstractSelectionConditionTemplate() {
        // required by Jackson!
    }

    protected AbstractSelectionConditionTemplate(String id, String label, Type type, Set<CandidateValue> candidateValues) {
        super(id, label, type);
        this.candidateValues = candidateValues;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), candidateValues);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AbstractSelectionConditionTemplate that = (AbstractSelectionConditionTemplate) o;
        return Objects.equals(candidateValues, that.candidateValues);
    }

    @Override
    public String toString() {
        return "AbstractSelectionConditionTemplate{" +
                super.toString() +
                ", candidateValues=" + candidateValues +
                '}';
    }

}
