package com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate;

import org.springframework.data.annotation.TypeAlias;

@TypeAlias("FreeTextConditionTemplate")
public class FreeTextConditionTemplate extends AbstractConditionTemplate {

    @SuppressWarnings("unused")
    private FreeTextConditionTemplate() {
        // required by Jackson!
    }

    public FreeTextConditionTemplate(String id, String label) {
        super(id, label, AbstractConditionTemplate.Type.FREE_TEXT);
    }

}
