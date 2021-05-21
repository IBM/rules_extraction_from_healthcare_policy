package com.ibm.wh.extractionservice.externalentity.groupAddition;

import com.ibm.wh.extractionservice.commons.externalentity.type.GroupExternalEntity;
import com.ibm.wh.extractionservice.commons.externalentity.type.IndividualExternalEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GroupAdditionAll {
    GroupExternalEntity groupAdditionEntry;
    List<IndividualExternalEntity> individualExternalEntities = new ArrayList<>();

    @SuppressWarnings("unused")
    protected GroupAdditionAll() {
        // required by Jackson!
    }

    public GroupAdditionAll(GroupExternalEntity groupAdditionEntry, List<IndividualExternalEntity> individualExternalEntities) {
        this.groupAdditionEntry = groupAdditionEntry;
        this.individualExternalEntities = individualExternalEntities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupAdditionAll that = (GroupAdditionAll) o;
        return groupAdditionEntry.equals(that.groupAdditionEntry) && individualExternalEntities.equals(that.individualExternalEntities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupAdditionEntry, individualExternalEntities);
    }

    @Override
    public String toString() {
        return "GroupAdditionAll{" +
                "groupAdditionEntry=" + groupAdditionEntry +
                ", individualExternalEntities=" + individualExternalEntities +
                '}';
    }
}
