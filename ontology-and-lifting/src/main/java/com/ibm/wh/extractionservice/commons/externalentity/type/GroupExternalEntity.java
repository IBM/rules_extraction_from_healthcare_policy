package com.ibm.wh.extractionservice.commons.externalentity.type;

import static com.ibm.wh.extractionservice.commons.utils.CollectionsUtils.*;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.value.CandidateValue;
import com.ibm.wh.extractionservice.commons.externalentity.ExternalEntity;

public class GroupExternalEntity extends ExternalEntity {

    private final Set<CandidateValue> members;

    private GroupExternalEntity() {
        // required by Jackson!
        members = Collections.emptySet();
    }

    public GroupExternalEntity(String entityId, Set<String> typesId, String displayName, Set<String> surfaceForms, String description, String tag, String claimValue, Set<CandidateValue> members) {
        super(entityId, typesId, displayName, surfaceForms, description, tag, claimValue, Type.GROUP);
        this.members = members;
    }

    public Set<CandidateValue> getMembers() {
        return members;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GroupExternalEntity that = (GroupExternalEntity) o;
        return Objects.equals(members, that.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), members);
    }

    @Override
    public GroupExternalEntity merge(ExternalEntity o) {
        if (!canBeMerged(o))
            throw new IllegalArgumentException(String.format("Impossible to merge entities [%s] and [%s]", this, o));

        GroupExternalEntity other = (GroupExternalEntity) o;
        return new GroupExternalEntity(
                firstOrSecondIfNull(this.getEntityId(), other.getEntityId()),
                unionAll(this.getEntityTypeIds(), other.getEntityTypeIds()),
                firstOrSecondIfNull(this.getDisplayName(), other.getDisplayName()),
                unionAll(this.getSurfaceForms(), other.getSurfaceForms()),
                firstOrSecondIfNull(this.getDescription(), other.getDescription()),
                firstOrSecondIfNull(this.getTag(), other.getTag()),
                firstOrSecondIfNull(this.getClaimValue(), other.getClaimValue()),
                unionAll(this.getMembers(), other.getMembers()));
    }

}
