package com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate;

import java.util.Set;

import org.springframework.data.annotation.TypeAlias;

import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.value.CandidateValue;

@TypeAlias("MultipleSelectionConditionTemplate")
public class MultipleSelectionConditionTemplate extends AbstractSelectionConditionTemplate {

    @SuppressWarnings("unused")
    private MultipleSelectionConditionTemplate() {
        // required by Jackson!
    }

    public MultipleSelectionConditionTemplate(String id, String label, Set<CandidateValue> candidateValues) {
        super(id, label, AbstractConditionTemplate.Type.MULTIPLE_SELECTION, candidateValues);
    }

}
