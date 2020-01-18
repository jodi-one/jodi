package one.jodi.core.targetcolumn.impl;

import one.jodi.core.extensions.contexts.FlagsDataStoreExecutionContext;
import one.jodi.core.extensions.contexts.FlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.contexts.UDFlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.strategies.FlagsStrategy;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;

import java.util.Set;

/**
 * This class implements a user defined flag and insert and update flags
 * strategy that simply echoes back the provided default values.
 *
 */
public class FlagsIDStrategy implements
        FlagsStrategy {

    /* (non-Javadoc)
     * @see one.jodi.core.extensions.strategies.FlagsStrategy#getUserDefinedFlags(one.jodi.core.extensions.contexts.FlagsDataStoreExecutionContext, one.jodi.core.extensions.contexts.UDFlagsTargetColumnExecutionContext, java.util.Collection)
     */
    @Override
    public Set<UserDefinedFlag> getUserDefinedFlags(
            final Set<UserDefinedFlag> defaultValues,
            final FlagsDataStoreExecutionContext tableContext,
            final UDFlagsTargetColumnExecutionContext columnContext) {
        return defaultValues;
    }

    /* (non-Javadoc)
     * @see one.jodi.core.extensions.strategies.FlagsStrategy#getInsertUpdateFlags(one.jodi.core.extensions.strategies.InsertUpdateFlags, one.jodi.core.extensions.contexts.FlagsDataStoreExecutionContext, one.jodi.core.extensions.contexts.UDFlagsTargetColumnExecutionContext)
     */
    @Override
    public TargetColumnFlags getTargetColumnFlags(
            TargetColumnFlags defaultValues,
            FlagsDataStoreExecutionContext tableContext,
            FlagsTargetColumnExecutionContext columnContext) {
        return defaultValues;
    }

}
