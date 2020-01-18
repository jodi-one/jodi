package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class ColumnAnnotations extends ObjectAnnotation {

    protected static final String DESCRIPTION = "Description";
    protected static final String BUSINESS_NAME = "Business Name";
    protected static final String BUSINESS_ABBREV = "Abbreviated Business Name";
    protected static final String IS_HIDDEN = "Hide";
    private static final Logger logger = LogManager.getLogger(ColumnAnnotations.class);
    private static final String ERROR_MESSAGE_80400 =
            "Unexpected result type for annotation with key '%1$s' in " +
                    "column '%2$s' of table '%3$s'. Type '%4$s' was expected.";
    private static final String ERROR_MESSAGE_80410 =
            "Unknown Annotation Key '%1$s' in column '%2$s' of table '%3$s'. " +
                    "This annotation will be ignored.";
    protected final Map<String, Object> annotations = new HashMap<>();
    private final TableAnnotations parent;
    private final ErrorWarningMessageJodi errorWarningMessages;

    protected ColumnAnnotations(final TableAnnotations parent, final String name,
                                final ErrorWarningMessageJodi errorWarningMessages) {
        super(name);
        this.parent = parent;
        this.errorWarningMessages = errorWarningMessages;
    }

    protected ColumnAnnotations(final TableAnnotations parent, final String name,
                                final String businessName,
                                final String abbreviatedBusinessName,
                                final String description,
                                final Boolean isHidden,
                                final ErrorWarningMessageJodi errorWarningMessages) {
        this(parent, name, errorWarningMessages);

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
        if (isHidden != null) {
            this.annotations.put(IS_HIDDEN.toLowerCase(), isHidden);
        }
    }

    public TableAnnotations getParent() {
        return parent;
    }

    public boolean isEmpty() {
        return this.annotations.isEmpty();
    }

    //
    // process column annotations
    //

    protected void initializeAnnotations(final Map<String, Object> annotations) {
        assert (this.annotations.isEmpty()) :
                "Annotations were already added for column " + getName() +
                        " of table " + this.parent.getName();
        this.annotations.putAll(getValidAnnotations(annotations, getKeyTypeMap()));
    }

    protected void merge(final ColumnAnnotations otherColumnAnnotations) {
        assert (otherColumnAnnotations != null) :
                "Attempt to merge null annotations for column " + getName();
        assert (this.getName().equals(otherColumnAnnotations.getName()) &&
                this.getParent().getName().equals(
                        otherColumnAnnotations.getParent().getName())) :
                "Attempt to merge annotations with different names for column " + getName();

        //add new or replace existing values
        this.annotations.putAll(otherColumnAnnotations.annotations);
    }

    protected void reportUnexpectedType(final String key, final Class<?> keyTypes) {
        String msg = errorWarningMessages.formatMessage(80400,
                ERROR_MESSAGE_80400, this.getClass(), key,
                getName(), this.parent.getName(),
                keyTypes.getSimpleName());
        errorWarningMessages.addMessage(
                errorWarningMessages.assignSequenceNumber(), msg,
                MESSAGE_TYPE.ERRORS);
        logger.error(msg);
    }

    protected void reportUndefinedAnnotation(final String key) {
        String msg = errorWarningMessages.formatMessage(80410,
                ERROR_MESSAGE_80410, this.getClass(), key,
                getName(), this.parent.getName());
        errorWarningMessages.addMessage(
                errorWarningMessages.assignSequenceNumber(), msg,
                MESSAGE_TYPE.WARNINGS);
        logger.warn(msg);
    }

    //
    // Generic annotations
    //

    public Optional<String> getDescription() {
        return Optional.ofNullable(
                ((String) annotations.get(DESCRIPTION.toLowerCase())));
    }

    public Optional<String> getBusinessName() {
        return Optional.ofNullable(
                ((String) annotations.get(BUSINESS_NAME.toLowerCase())));
    }

    public Optional<String> getAbbreviatedBusinessName() {
        return Optional.ofNullable(
                ((String) annotations.get(BUSINESS_ABBREV.toLowerCase())));
    }

    public Optional<Boolean> isHidden() {
        return Optional.ofNullable(
                ((Boolean) annotations.get(IS_HIDDEN.toLowerCase())));
    }

}