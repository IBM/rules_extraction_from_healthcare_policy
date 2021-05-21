package com.ibm.wh.extractionservice.commons.benefitrule.conditiontemplate.value;

import java.util.Objects;

public class CandidateValue {

    private String id;
    // FIXME: rename to displayName
    private String label;
    private String claimValue;
    private Type type;

    @SuppressWarnings("unused")
    protected CandidateValue() {
        // required by Jackson!
    }

    public CandidateValue(String id, String label, String claimValue, Type type) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.claimValue = claimValue;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getClaimValue() {
        return claimValue;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateValue that = (CandidateValue) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(label, that.label) &&
                Objects.equals(claimValue, that.claimValue) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, claimValue, type);
    }

    @Override
    public String toString() {
        return "CandidateValue{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", claimValue='" + claimValue + '\'' +
                ", type=" + type +
                '}';
    }

    public enum Type {

        SINGLE, GROUP, EXTRACTED

    }

}