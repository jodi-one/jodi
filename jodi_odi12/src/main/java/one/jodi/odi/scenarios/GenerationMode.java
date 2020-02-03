package one.jodi.odi.scenarios;

/**
 * Indicates the approach taken when generating ETL transformations. <code>
 * GenerationMode.INCREMENTAL</code> attempts to update the existing
 * transformations without deleting and recreating.
 */
public enum GenerationMode {
    REPLACE(1), INCREMENTAL(2), REGENERATE(3);

    private int value;

    GenerationMode(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
