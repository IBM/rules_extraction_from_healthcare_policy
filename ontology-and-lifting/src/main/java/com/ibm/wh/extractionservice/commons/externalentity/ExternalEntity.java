package com.ibm.wh.extractionservice.commons.externalentity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity.Type;
import com.ibm.wh.extractionservice.commons.externalentity.type.GroupExternalEntity;
import com.ibm.wh.extractionservice.commons.externalentity.type.IndividualExternalEntity;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GroupExternalEntity.class, name = Type.GROUP_AS_STRING),
        @JsonSubTypes.Type(value = IndividualExternalEntity.class, name = Type.INDIVIDUAL_AS_STRING),
})
@TypeAlias("ExternalEntity")
public abstract class ExternalEntity {

    @Id
    private String id;
    private String entityId;
    private String description;
    private String displayName;
    private final Set<String> surfaceForms;
    private String tag;
    private String claimValue;
    private Instant timestamp;
    private final Set<String> entityTypeIds;
    private Type type;

    protected ExternalEntity() {
        // required by Jackson!

        // if not specified differently, assume that the entity is uploaded
        entityTypeIds = new HashSet<>();
        surfaceForms = new HashSet<>();
    }

    protected ExternalEntity(String entityId, Set<String> entityTypeIds, String displayName, Set<String> surfaceForms, String description, String tag, String claimValue, Type type) {
        this.entityId = entityId;
        this.entityTypeIds = entityTypeIds;
        this.description = description;
        this.displayName = displayName;
        this.surfaceForms = surfaceForms;
        this.tag = tag;
        this.timestamp = Instant.now();
        this.claimValue = claimValue;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getSurfaceForms() {
        return surfaceForms;
    }

    public String getTag() {
        return tag;
    }

    public String getClaimValue() {
        return claimValue;
    }

    public Set<String> getEntityTypeIds() {
        return entityTypeIds;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalEntity that = (ExternalEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(entityId, that.entityId) &&
                Objects.equals(description, that.description) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(surfaceForms, that.surfaceForms) &&
                Objects.equals(tag, that.tag) &&
                Objects.equals(claimValue, that.claimValue) &&
                Objects.equals(entityTypeIds, that.entityTypeIds) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, entityId, description, displayName, surfaceForms, tag, claimValue, entityTypeIds, type);
    }

    public abstract ExternalEntity merge(ExternalEntity other);

    public boolean canBeMerged(ExternalEntity o) {
        // it can be merged if all the single values are shared
        return o != null
                && this.getClass() == o.getClass()
                && equalsOrOneIsNull(this.getEntityId(), o.getEntityId())
                && equalsOrOneIsNull(this.getDisplayName(), o.getDisplayName())
                && equalsOrOneIsNull(this.getDescription(), o.getDescription())
                && equalsOrOneIsNull(this.getTag(), o.getTag())
                && equalsOrOneIsNull(this.getClaimValue(), o.getClaimValue());
    }

    protected static boolean equalsOrOneIsNull(Object one, Object other) {
        if (one != null && other != null) return Objects.equals(one, other);
        return true;
    }

    protected static <T> T firstOrSecondIfNull(T first, T second) {
        if (first == null) return second;
        return first;
    }

    public enum Type {

        DISCOVERED, GROUP, INDIVIDUAL;

        public static final String GROUP_AS_STRING = "GROUP";
        public static final String INDIVIDUAL_AS_STRING = "INDIVIDUAL";
        public static final String DISCOVERED_AS_STRING = "DISCOVERED";
    }

}
