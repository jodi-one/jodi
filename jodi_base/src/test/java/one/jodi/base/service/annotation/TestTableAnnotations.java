package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;

import java.util.Map;
import java.util.Optional;

public class TestTableAnnotations extends TableAnnotations {

    private final static String TYPE = "Type";
    private final static String IS_BASE = "Is Base";
    private final static String IS_HIDDEN = "Hide";
    private final static String IS_RAGGED_DIMENSION = "Is Ragged Dimension";

    // Boolean Annotation keys
    private final static String[] bKeys =
            new String[]{IS_BASE, IS_HIDDEN, IS_RAGGED_DIMENSION};
    // String Annotation keys
    private final static String[] sKeys =
            new String[]{TYPE, DESCRIPTION, BUSINESS_NAME, BUSINESS_ABBREV};
    // Integer Annotation keys
    private final static String[] iKeys = new String[]{};
    // Array
    private final static String[] aKeys = new String[]{};

    TestTableAnnotations(final String schema, final String name,
                         final AnnotationFactory annotationFactor,
                         final ErrorWarningMessageJodi errorWarningMessages) {
        super(schema, name, annotationFactor, errorWarningMessages);
    }

    TestTableAnnotations(final String schema, final String name,
                         final String businessName,
                         final String abbreviatedBusinessName,
                         final String description,
                         final AnnotationFactory annotationFactor,
                         final ErrorWarningMessageJodi errorWarningMessages) {
        super(schema, name, businessName, abbreviatedBusinessName, description,
                annotationFactor, errorWarningMessages);
    }

    protected Map<String, Class<?>> getKeyTypeMap() {
        // setup annotation key and expected type association
        return createKeyTypeMap(bKeys, sKeys, iKeys, aKeys);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public void initializeAnnotations(final Map<String, Object> annotations) {
        super.initializeAnnotations(annotations);
    }

    protected void addColumnAnnotations(final String columnName,
                                        final Map<String, Object> annotations) {
        super.addColumnAnnotations(columnName, annotations);
    }

    // String Annotations

    public Optional<String> getType() {
        Optional<String> result = Optional.empty();
        String value = (String) annotations.get(TYPE.toLowerCase());
        // map empty string to null as well
        if (value != null && !value.trim().isEmpty()) {
            result = Optional.ofNullable(value);
        }
        return result;
    }

    // Boolean Annotations

    public Optional<Boolean> isBase() {
        return Optional.ofNullable(
                ((Boolean) annotations.get(IS_BASE.toLowerCase())));
    }

    public Optional<Boolean> isHidden() {
        return Optional.ofNullable(
                ((Boolean) annotations.get(IS_HIDDEN.toLowerCase())));
    }

    public Optional<Boolean> isRaggedDimension() {
        return Optional.ofNullable(
                ((Boolean) annotations.get(IS_RAGGED_DIMENSION.toLowerCase())));
    }

}
