package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TestColumnAnnotations extends ColumnAnnotations {

    // Boolean
    private static final String IS_HIDDEN = "Hide";
    private static final String IS_DISPLAY = "Is Display Column";

    // Integer
    private static final String TARGET_LEVEL = "Target Level";

    // Array
    private static final String TARGET_LEVELS = "Target Levels";

    private static final String[] stringKeys = new String[]{DESCRIPTION, BUSINESS_NAME, BUSINESS_ABBREV};

    // Boolean Annotation keys
    private static final String[] bKeys = new String[]{IS_HIDDEN, IS_DISPLAY};

    // Integer Annotation keys
    private static final String[] iKeys = new String[]{TARGET_LEVEL};

    // Integer Annotation keys
    private static final String[] aKeys = new String[]{TARGET_LEVELS};

    TestColumnAnnotations(final TableAnnotations tableAnnotations, final String name,
                          final ErrorWarningMessageJodi errorWarningMessages) {
        super(tableAnnotations, name, errorWarningMessages);
    }

    TestColumnAnnotations(final TableAnnotations parent, final String name, final String businessName,
                          final String abbreviatedBusinessName, final String description, final Boolean isHidden,
                          final ErrorWarningMessageJodi errorWarningMessages) {
        super(parent, name, businessName, abbreviatedBusinessName, description, isHidden, errorWarningMessages);
    }

    @Override
    protected Map<String, Class<?>> getKeyTypeMap() {
        // setup annotation key and expected type association
        return createKeyTypeMap(bKeys, stringKeys, iKeys, aKeys);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void initializeAnnotations(final Map<String, Object> annotations) {
        super.initializeAnnotations(annotations);
    }

    @Override
    public void merge(final ColumnAnnotations otherColumnAnnotations) {
        super.merge(otherColumnAnnotations);
    }

    // Boolean Annotations

    @Override
    public Optional<Boolean> isHidden() {
        return Optional.ofNullable(((Boolean) annotations.get(IS_HIDDEN.toLowerCase())));
    }

    public Optional<Boolean> isDisplayColumn() {
        return Optional.ofNullable(((Boolean) annotations.get(IS_DISPLAY.toLowerCase())));
    }

    // Integer Annotation

    public Optional<Integer> targetLevel() {
        return Optional.ofNullable(((Integer) annotations.get(TARGET_LEVEL.toLowerCase())));
    }

    @SuppressWarnings("unchecked")
    public Optional<List<String>> targetLevels() {
        return Optional.ofNullable(((List<String>) annotations.get(TARGET_LEVELS.toLowerCase())));
    }

}
