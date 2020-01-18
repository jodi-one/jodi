package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;

import java.util.Map;

public class TestVariableAnnotations extends VariableAnnotations {

    private final static String NAME = "Name";
    private final static String DESCRIPTION = "Description";
    private final static String IS_SESSION = "Is Session";
    private final static String ANY_USER = "Any User";
    private final static String SECURITY_SENSITIVE = "Security Sensitive";
    private final static String DYNAMIC_INITIALIZATION_EXPRESSION =
            "Dynamic Initialization Expression";
    private final static String DEFAULT_INITIALIZATION =
            "Default Initialization";


    // Boolean Annotation keys
    private final static String[] bKeys = new String[]{
            IS_SESSION, ANY_USER, SECURITY_SENSITIVE};

    // String Annotation keys
    private final static String[] sKeys = new String[]{
            NAME, DESCRIPTION, DYNAMIC_INITIALIZATION_EXPRESSION,
            DEFAULT_INITIALIZATION};

    // Integer Annotation keys
    private final static String[] iKeys = new String[]{};

    // Array Annotation keys
    private final static String[] aKeys = new String[]{};

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
