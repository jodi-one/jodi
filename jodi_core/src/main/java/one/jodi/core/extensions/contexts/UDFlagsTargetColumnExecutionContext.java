package one.jodi.core.extensions.contexts;

import one.jodi.core.extensions.strategies.FlagsStrategy;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;

import java.util.Set;

/**
 * This interface is used to provide contextual information for
 * {@link FlagsStrategy FlagsStrategy}
 * for the selected target columns. This interface is only used when determining
 * user-defined flags
 * {@link FlagsStrategy#getUserDefinedFlags(java.util.Set, FlagsDataStoreExecutionContext, UDFlagsTargetColumnExecutionContext)
 * FlagsStrategy#getUserDefinedFlags(...)} for a target column.
 *
 */
public interface UDFlagsTargetColumnExecutionContext extends FlagsTargetColumnExecutionContext {

    /**
     * @return Object containing multiple flags that are going to be
     * associated with the target column in mappings definition.
     */
    TargetColumnFlags getTargetColumnFlags();

    Set<UserDefinedFlag> getUserDefinedFlags();

}
