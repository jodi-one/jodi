package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TestColumnAnnotations extends ColumnAnnotations {

    // Boolean
    private final static String IS_HIDDEN = "Hide";
    private final static String IS_DISPLAY = "Is Display Column";

    // Integer
    private final static String TARGET_LEVEL = "Target Level";

    // Array
    private final static String TARGET_LEVELS = "Target Levels";

    private final static String[] stringKeys =
            new String[]{DESCRIPTION, BUSINESS_NAME, BUSINESS_ABBREV};

    // Boolean Annotation keys
    private final static String[] bKeys =
            new String[]{IS_HIDDEN, IS_DISPLAY};

    // Integer Annotation keys
    private final static String[] iKeys =
            new String[]{TARGET_LEVEL};

    // Integer Annotation keys
    private final static String[] aKeys =
            new String[]{TARGET_LEVELS};

    TestColumnAnnotations(final TableAnnotations tableAnnotations, final String name,
                          final ErrorWarningMessageJodi errorWarningMessages) {
        super(tableAnnotations, name, errorWarningMessages);
    }

    TestColumnAnnotations(final TableAnnotations parent, final String name,
                          final String businessName,
                          final String abbreviatedBusinessName,
                          final String description,
                          final Boolean isHidden,
                          final ErrorWarningMessageJodi errorWarningMessages) {
        super(parent, name, businessName, abbreviatedBusinessName, description, isHidden,
                errorWarningMessages);
    }

    protected Map<String, Class<?>> getKeyTypeMap() {
        // setup annotation key and expected type association
        return createKeyTypeMap(bKeys, stringKeys, iKeys, aKeys);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public void initializeAnnotations(final Map<String, Object> annotations) {
        super.initializeAnnotations(annotations);
    }

    public void merge(final ColumnAnnotations otherColumnAnnotations) {
        super.merge(otherColumnAnnotations);
    }

    // Boolean Annotations

    public Optional<Boolean> isHidden() {
        return Optional.ofNullable(
                ((Boolean) annotations.get(IS_HIDDEN.toLowerCase())));
    }

    public Optional<Boolean> isDisplayColumn() {
        return Optional.ofNullable(
                ((Boolean) annotations.get(IS_DISPLAY.toLowerCase())));
    }

    // Integer Annotation

    public Optional<Integer> targetLevel() {
        return Optional.ofNullable(
                ((Integer) annotations.get(TARGET_LEVEL.toLowerCase())));
    }

    @SuppressWarnings("unchecked")
    public Optional<List<String>> targetLevels() {
        return Optional.ofNullable(
                ((List<String>) annotations.get(TARGET_LEVELS.toLowerCase())));
    }

}
