package one.jodi.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Method level annotation that defines transaction semantics. Range of values
 * defined by {@link TransactionAttributeType}. Defaults to <code>
 * TransactionAttributeType.REQUIRED</code>
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface TransactionAttribute {
    TransactionAttributeType value() default TransactionAttributeType.REQUIRED;
}
