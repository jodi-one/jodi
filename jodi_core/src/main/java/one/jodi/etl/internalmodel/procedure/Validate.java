package one.jodi.etl.internalmodel.procedure;

import one.jodi.base.error.ErrorWarningMessageJodi;

public interface Validate {
    boolean validate(ErrorWarningMessageJodi errorWarningMessages);
}
