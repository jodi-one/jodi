package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public abstract class TableAnnotations extends ObjectAnnotation {

    protected static final String DESCRIPTION = "Description";
    protected static final String BUSINESS_NAME = "Business Name";
    protected static final String BUSINESS_ABBREV = "Abbreviated Business Name";
    private final static Logger logger = LogManager.getLogger(TableAnnotations.class);
    private static final String ERROR_MESSAGE_80700 =
            "Unexpected result type for annotation with key '%1$s' in " +
                    "table '%2$s'. Type '%3$s' was expected.";

    private static final String ERROR_MESSAGE_80710 =
            "Unknown Annotation Key '%1$s' in table '%2$s'. " +
                    "This annotation will be ignored.";
    protected final Map<String, Object> annotations = new HashMap<>();
    private final String schema;
    private final Map<String, ColumnAnnotations> columnAnnotations = new HashMap<>();
    private final AnnotationFactory annotationFactory;
    private final ErrorWarningMessageJodi errorWarningMessages;

    protected TableAnnotations(final String schema, final String name,
                               final AnnotationFactory annotationFactory,
                               final ErrorWarningMessageJodi errorWarningMessages) {
        super(name);
        assert (schema != null && !schema.isEmpty());
        this.schema = schema;
        this.annotationFactory = annotationFactory;
        this.errorWarningMessages = errorWarningMessages;
    }

    // used when creating default annotations using legacy comment metadata format
    protected TableAnnotations(final String schema, final String name,
                               final String businessName,
                               final String abbreviatedBusinessName,
                               final String description,
                               final AnnotationFactory annotationFactory,
                               final ErrorWarningMessageJodi errorWarningMessages) {
        this(schema, name, annotationFactory, errorWarningMessages);

        // FYI: TYPE default is currently not defined on purpose
        if (businessName != null && !businessName.trim().isEmpty()) {
            this.annotations.put(BUSINESS_NAME.toLowerCase(), businessName.trim());
        }
        if (abbreviatedBusinessName != null && !abbreviatedBusinessName.trim().isEmpty()) {
            this.annotations.put(BUSINESS_ABBREV.toLowerCase(),
                    abbreviatedBusinessName.trim());
        }
        if (description != null) {
            this.annotations.put(DESCRIPTION.toLowerCase(), description.trim());
        }
    }

    public String getSchemaName() {
        return schema;
    }

    public boolean isEmpty() {
        return this.annotations.isEmpty() &&
                // handles calculated columns only (error jodi-575)
                this.columnAnnotations.isEmpty();
    }

    //
    // process table annotations
    //

    public void initializeAnnotations(final Map<String, Object> annotations) {
        assert (this.annotations.isEmpty()) :
                "Annotations were already added for table " + getName();
        this.annotations.putAll(getValidAnnotations(annotations, getKeyTypeMap()));
    }

    public void merge(final TableAnnotations otherTableAnnotations) {
        assert (otherTableAnnotations != null) :
                "Attempt to merge null annotations for table " + getName();
        assert (this.getName().equals(otherTableAnnotations.getName()) &&
                this.getSchemaName().equals(otherTableAnnotations.getSchemaName())) :
                "Attempt to merge annotations with different names for table " + getName();

        //add new or replace existing values
        this.annotations.putAll(otherTableAnnotations.annotations);

        // fine grained merging
        for (Entry<String, ColumnAnnotations> e :
                otherTableAnnotations.columnAnnotations.entrySet()) {
//         assert(!this.columnAnnotations.containsKey(e.getKey())) :
//            "Column merge use case not implemented for table " + getName() + ".";
            if (this.columnAnnotations.containsKey(e.getKey())) {
                this.columnAnnotations.get(e.getKey()).merge(e.getValue());
            } else {
                this.columnAnnotations.put(e.getKey(), e.getValue());
            }
        }
    }

    protected void reportUnexpectedType(final String key, final Class<?> keyTypes) {
        String msg = errorWarningMessages.formatMessage(80700,
                ERROR_MESSAGE_80700, this.getClass(), key, getName(),
                keyTypes.getSimpleName());
        errorWarningMessages.addMessage(
                errorWarningMessages.assignSequenceNumber(), msg,
                MESSAGE_TYPE.ERRORS);
        logger.error(msg);
    }

    //
    // process column annotations
    //

    protected void reportUndefinedAnnotation(final String key) {
        String msg = errorWarningMessages.formatMessage(80710,
                ERROR_MESSAGE_80710, this.getClass(), key, getName());
        errorWarningMessages.addMessage(
                errorWarningMessages.assignSequenceNumber(), msg,
                MESSAGE_TYPE.WARNINGS);
        logger.warn(msg);
    }

    protected void addColumnAnnotations(final String columnName,
                                        final Map<String, Object> annotations) {
        ColumnAnnotations cAnnotations =
                annotationFactory.createColumnAnnotations(this, columnName);
        cAnnotations.initializeAnnotations(annotations);
        this.columnAnnotations.put(columnName, cAnnotations);
    }

    protected void addColumnAnnotations(final ColumnAnnotations annotations) {
        // replace if column annotation already exists
        this.columnAnnotations.put(annotations.getName(), annotations);
    }

    public Map<String, ColumnAnnotations> getColumnAnnotations() {
        return Collections.unmodifiableMap(this.columnAnnotations);
    }

    public Optional<String> getDescription() {
        Optional<String> result = Optional.empty();
        String value = (String) annotations.get(DESCRIPTION.toLowerCase());
        // map empty string to null as well
        if (value != null && !value.trim().isEmpty()) {
            result = Optional.ofNullable(value);
        }
        return result;
    }

    public Optional<String> getBusinessName() {
        Optional<String> result = Optional.empty();
        String value = (String) annotations.get(BUSINESS_NAME.toLowerCase());
        // map empty string to null as well
        if (value != null && !value.trim().isEmpty()) {
            result = Optional.ofNullable(value);
        }
        return result;
    }

    public Optional<String> getAbbreviatedBusinessName() {
        Optional<String> result = Optional.empty();
        String value = (String) annotations.get(BUSINESS_ABBREV.toLowerCase());
        // map empty string to null as well
        if (value != null && !value.trim().isEmpty()) {
            result = Optional.ofNullable(value);
        }
        return result;
    }

}