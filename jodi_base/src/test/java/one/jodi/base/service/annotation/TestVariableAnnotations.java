package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;

import java.util.Map;

public class TestVariableAnnotations extends VariableAnnotations {

    private static final String NAME = "Name";
    private static final String DESCRIPTION = "Description";
    private static final String IS_SESSION = "Is Session";
    private static final String ANY_USER = "Any User";
    private static final String SECURITY_SENSITIVE = "Security Sensitive";
    private static final String DYNAMIC_INITIALIZATION_EXPRESSION =
            "Dynamic Initialization Expression";
    private static final String DEFAULT_INITIALIZATION =
            "Default Initialization";


    // Boolean Annotation keys
    private static final String[] bKeys = new String[]{
            IS_SESSION, ANY_USER, SECURITY_SENSITIVE};

    // String Annotation keys
    private static final String[] sKeys = new String[]{
            NAME, DESCRIPTION, DYNAMIC_INITIALIZATION_EXPRESSION,
            DEFAULT_INITIALIZATION};

    // Integer Annotation keys
    private static final String[] iKeys = new String[]{};

    // Array Annotation keys
    private static final String[] aKeys = new String[]{};

    protected TestVariableAnnotations(final String name,
                                      final ErrorWarningMessageJodi errorWarningMessages) {
        super(name, errorWarningMessages);
    }

    protected Map<String, Class<?>> getKeyTypeMap() {
        return createKeyTypeMap(bKeys, sKeys, iKeys, aKeys);
    }

    public void initializeAnnotations(final Map<String, Object> annotations) {
        super.initializeAnnotations(annotations);
    }

    public boolean isValid() {
        return super.isValid();
    }
}
