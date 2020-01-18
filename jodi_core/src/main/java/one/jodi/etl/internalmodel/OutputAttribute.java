package one.jodi.etl.internalmodel;

import java.util.Map;

public interface OutputAttribute {

    String getName();

    Map<String, String> getExpressions();

    /**
     * Determine if there are qualified expressions, e.g. value is non-null.
     *
     * @return hasQualifiedExpressions
     */
    boolean hasQualifiedExpressions();

}
