package com.ibm.wh.extractionservice.commons.externalentity.type;

import static com.ibm.wh.extractionservice.commons.utils.CollectionsUtils.*;

import java.util.Set;

import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity;

public class IndividualExternalEntity extends ExternalEntity {

    private IndividualExternalEntity() {
        // Required by Jackson!
    }

    public IndividualExternalEntity(String entityId, Set<String> types, String displayName, Set<String> surfaceForms, String description, String tag, String claimValue) {
        super(entityId, types, displayName, surfaceForms, description, tag, claimValue, Type.INDIVIDUAL);
    }

    @Override
    public IndividualExternalEntity merge(ExternalEntity o) {
        if (!canBeMerged(o))
            throw new IllegalArgumentException(String.format("Impossible to merge entities [%s] and [%s]", this, o));

        IndividualExternalEntity other = (IndividualExternalEntity) o;
        return new IndividualExternalEntity(
                firstOrSecondIfNull(this.getEntityId(), other.getEntityId()),
                unionAll(this.getEntityTypeIds(), other.getEntityTypeIds()),
                firstOrSecondIfNull(this.getDisplayName(), other.getDisplayName()),
                unionAll(this.getSurfaceForms(), other.getSurfaceForms()),
                firstOrSecondIfNull(this.getDescription(), other.getDescription()),
                firstOrSecondIfNull(this.getTag(), other.getTag()),
                firstOrSecondIfNull(this.getClaimValue(), other.getClaimValue()));
    }

}
