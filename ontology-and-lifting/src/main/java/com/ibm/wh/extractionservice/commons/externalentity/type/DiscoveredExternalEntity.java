package com.ibm.wh.extractionservice.commons.externalentity.type;

import static com.ibm.wh.extractionservice.commons.utils.CollectionsUtils.*;

import java.util.Set;

import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity;

public class DiscoveredExternalEntity extends ExternalEntity {

    private Set<String> externalEntityIds;

    private DiscoveredExternalEntity() {
        // Required by Jackson!
    }

    public DiscoveredExternalEntity(String entityId, Set<String> types, String displayName, Set<String> surfaceForms, String description, String tag, String claimValue, Set<String> externalEntityIds) {
        super(entityId, types, displayName, surfaceForms, description, tag, claimValue, Type.DISCOVERED);
        this.externalEntityIds = externalEntityIds;
    }

    public Set<String> getExternalEntityIds() {
        return externalEntityIds;
    }

    @Override
    public DiscoveredExternalEntity merge(ExternalEntity o) {
        if (!canBeMerged(o))
            throw new IllegalArgumentException(String.format("Impossible to merge entities [%s] and [%s]", this, o));

        DiscoveredExternalEntity other = (DiscoveredExternalEntity) o;
        return new DiscoveredExternalEntity(
                firstOrSecondIfNull(this.getEntityId(), other.getEntityId()),
                unionAll(this.getEntityTypeIds(), other.getEntityTypeIds()),
                firstOrSecondIfNull(this.getDisplayName(), other.getDisplayName()),
                unionAll(this.getSurfaceForms(), other.getSurfaceForms()),
                firstOrSecondIfNull(this.getDescription(), other.getDescription()),
                firstOrSecondIfNull(this.getTag(), other.getTag()),
                firstOrSecondIfNull(this.getClaimValue(), other.getClaimValue()),
                unionAll(this.getExternalEntityIds(), other.getExternalEntityIds()));
    }

}
