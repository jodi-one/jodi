package one.jodi.etl.km;

/**
 * Interface used to discriminate the various kinds of Knowledge Modules.
 */
public enum KnowledgeModuleType {
    Loading,
    Integration,
    Check,
    Journalization,
    Service,
    Unknown
}
