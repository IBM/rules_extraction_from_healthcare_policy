package com.ibm.wh.extractionservice.externalentity.groupAddition;

import java.util.Objects;
import java.util.Set;

public class GroupAdditionConfig {

    private Set<String> typesId;
    private String displayName;
    private String description;
    private String entityId;
    private String tag;
    private String namespace;
    private Set<GroupAdditionItem> proposedMembers;

    @SuppressWarnings("unused")
    protected GroupAdditionConfig() {
        // required by Jackson!
    }


    public Set<String> getTypesId() {
        return typesId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getTag() {
        return tag;
    }

    public Set<GroupAdditionItem> getProposedMembers() {
        return proposedMembers;
    }

    @Override
    public String toString() {
        return "GroupAddition{" +
                ", typesId=" + typesId +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", entityId='" + entityId + '\'' +
                ", namespace='" + namespace + '\'' +
                ", tag='" + tag + '\'' +
                ", proposedMembers=" + proposedMembers +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupAdditionConfig that = (GroupAdditionConfig) o;
        return Objects.equals(typesId, that.typesId) && Objects.equals(displayName, that.displayName)
                && Objects.equals(description, that.description) && Objects.equals(entityId, that.entityId)
                && Objects.equals(tag, that.tag) && Objects.equals(namespace, that.namespace)
                && Objects.equals(proposedMembers, that.proposedMembers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typesId, displayName, description, entityId, tag, namespace, proposedMembers);
    }


}
