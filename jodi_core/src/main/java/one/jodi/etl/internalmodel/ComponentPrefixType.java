package one.jodi.etl.internalmodel;

public enum ComponentPrefixType {
    FILTER("F"), DATASET("D"), LOOKUP("L"), SETCOMPONENT("SC"), TARGET_EXPRESSIONS("TARGET_EXPRESSIONS"), JOIN("J"), DISTINCT("DSTNCT"), AGGREGATE("AGG"), PIVOT("PVT"), UNPIVOT("UNPVT"), SUBQUERY("SQRY");
    private String abbreviation;

    ComponentPrefixType(final String a) {
        this.abbreviation = a;
    }

    public String getAbbreviation() {
        return this.abbreviation;
    }
}