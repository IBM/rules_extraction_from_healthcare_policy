package com.ibm.wh.extractionservice.ontology.search;

public final class OntologySearchResult implements Comparable<OntologySearchResult> {

    private final String uri;
    private final double score;

    public OntologySearchResult(String uri, double score) {
        this.uri = uri;
        this.score = score;
    }

    /**
     * @return uri: the URI of the Resource that matched the search
     */
    public String getUri() {
        return uri;
    }

    /**
     * @return score: the Score returned by Lucene (see https://lucene.apache.org/core/3_5_0/scoring.html).
     * Use carefully:
     * <ul>
     * <li><a href="https://lucene.apache.org/core/3_5_0/scoring.html">Apache Lucene - Scoring</a></li>
     * <li><a href="https://stackoverflow.com/questions/5379176/how-to-normalize-lucene-scores/5379786#5379786">There is no good standard way to normalize scores with lucene</a></li>
     * </ul>
     */
    public double getScore() {
        return score;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(score);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        OntologySearchResult other = (OntologySearchResult) obj;
        if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score)) return false;
        if (uri == null) {
            return other.uri == null;
        } else return uri.equals(other.uri);
    }

    @Override
    public String toString() {
        return "OntologySearchResult [uri=" + uri + ", score=" + score + "]";
    }

    @Override
    public int compareTo(OntologySearchResult other) {
        return Double.compare(this.score, other.score);
    }

}
