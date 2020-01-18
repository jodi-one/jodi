package one.jodi.core.extensions.strategies;

import one.jodi.core.extensions.contexts.FlagsDataStoreExecutionContext;
import one.jodi.core.extensions.contexts.FlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.contexts.UDFlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;

import java.util.Set;

/**
 * This interface defines the plug-in for determining the insert, update, update
 * key and mandatory flags and the user-defined flags. It is used to implement
 * the default flags policy and a custom policy. The default policy plug-in is
 * always executed before the custom plug-in is executed.
 *
 */
public interface FlagsStrategy {

    /**
     * Determines a set of user-defined flags for a given column and execution
     * context. These flags typically are used in conjunction with custom
     * knowledge modules.
     *
     * @param defaultValues          is <code>null</code> when passed into the default strategy;
     *                               the value is the result of the default strategy when passed to
     *                               the custom strategy. In this case the value will not be
     *                               <code>null</code>.
     * @param targetDataStoreContext contextual information for the strategy that describe the
     *                               target data store.
     * @param targetColumnContext    contextual information for the strategy with focus on the
     *                               target column for which operation is performed.
     * @return Set of user defined flags for the target column of the target
     * data store. The set may be empty.
     * @throws IncorrectCustomStrategyException if used-defined flags set is <code>null</code>.
     */
    Set<UserDefinedFlag> getUserDefinedFlags(
            Set<UserDefinedFlag> defaultValues,
            FlagsDataStoreExecutionContext targetDataStoreContext,
            UDFlagsTargetColumnExecutionContext targetColumnContext);

    /**
     * @param defaultValues          is the explicitly defined update key and mandatory flags or
     *                               <code>null</code> when passed into the default strategy; the
     *                               value is the result of the default strategy when passed to the
     *                               custom strategy. In this case the value will not be
     *                               <code>null</code>.
     * @param targetDataStoreContext contextual information for the strategy that describe the
     *                               target data store
     * @param targetColumnContext    contextual information for the strategy with focus on the
     *                               target column for which operation is performed
     * @return Object with flags (insert, update, update key, mandatory) for the
     * target column in the target data store defined in the above
     * parameters.
     * @throws IncorrectCustomStrategyException if return value is <code>null</code>.
     */
    TargetColumnFlags getTargetColumnFlags(TargetColumnFlags defaultValues,
                                           FlagsDataStoreExecutionContext targetDataStoreContext,
                                           FlagsTargetColumnExecutionContext targetColumnContext);

}
