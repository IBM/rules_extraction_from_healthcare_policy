package com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate;

import java.util.Set;

import org.springframework.data.annotation.TypeAlias;

import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.value.CandidateValue;

@TypeAlias("SingleSelectionConditionTemplate")
public class SingleSelectionConditionTemplate extends AbstractSelectionConditionTemplate {

    @SuppressWarnings("unused")
    private SingleSelectionConditionTemplate() {
        // required by Jackson!
    }

    public SingleSelectionConditionTemplate(String id, String label, Set<CandidateValue> candidateValues) {
        super(id, label, AbstractConditionTemplate.Type.SINGLE_SELECTION, candidateValues);
    }

}
