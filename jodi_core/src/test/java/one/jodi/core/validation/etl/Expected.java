package one.jodi.core.validation.etl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to unit test cases to specify expected warnings and errors catalogued by {@link ETLValidator} methods {@link ETLValidatorImpl#getErrorMessages()} and {@link ETLValidatorImpl#getWarningMessages()}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Expected {
    int[] errors() default {};

    int[] warnings() default {};

    int[] packageSequences() default {};
}

