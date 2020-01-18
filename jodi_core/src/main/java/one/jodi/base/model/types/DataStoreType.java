package one.jodi.base.model.types;

/**
 * Defines the typical DW layers.
 *
 */
public enum DataStoreType {
    // THIRD_NORMAL_FORM, -- potential value
    // THIRD_NORMAL_FORM_VERSIONED, -- potential value
    FACT,
    DIMENSION,
    SLOWLY_CHANGING_DIMENSION,
    HELPER,
    UNKNOWN

}