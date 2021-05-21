package com.ibm.wh.extractionservice.externalentity.groupAddition;

import java.util.Objects;

public class GroupAdditionItem {
    private GroupAdditionItemType groupAdditionItemType;
    private String groupAdditionBeginCode;
    private String groupAdditionEndCode;

    @SuppressWarnings("unused")
    protected GroupAdditionItem() {
        // required by Jackson!
    }


    public GroupAdditionItemType getGroupAdditionItemType() {
        return groupAdditionItemType;
    }

    public String getGroupAdditionBeginCode() {
        return groupAdditionBeginCode;
    }

    public String getGroupAdditionEndCode() {
        return groupAdditionEndCode;
    }

    public void setGroupAdditionEndCode(String groupAdditionEndCode) {
        this.groupAdditionEndCode = groupAdditionEndCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupAdditionItem that = (GroupAdditionItem) o;
        return groupAdditionItemType == that.groupAdditionItemType && Objects.equals(groupAdditionBeginCode, that.groupAdditionBeginCode) && Objects.equals(groupAdditionEndCode, that.groupAdditionEndCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupAdditionBeginCode, groupAdditionEndCode, groupAdditionItemType);
    }

    @Override
    public String toString() {
        return "GroupAdditionItemType{" +
                "groupAdditionBeginCode='" + groupAdditionBeginCode + '\'' +
                "groupAdditionBeginCode='" + groupAdditionEndCode + '\'' +
                ", groupAdditionItemType=" + groupAdditionItemType +
                '}';
    }

    public enum GroupAdditionItemType {
        SINGLE_CODE, RANGE
    }
}