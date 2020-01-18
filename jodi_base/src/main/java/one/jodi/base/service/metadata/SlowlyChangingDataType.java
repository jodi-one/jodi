package one.jodi.base.service.metadata;

/**
 * SCD type 2 columns classification types
 */
public enum SlowlyChangingDataType {
    SURROGATE_KEY,
    NATURAL_KEY,
    OVERWRITE_ON_CHANGE,
    ADD_ROW_ON_CHANGE,
    CURRENT_RECORD_FLAG,
    START_TIMESTAMP,
    END_TIMESTAMP
}
