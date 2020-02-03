package one.jodi.core.transformation.impl;

import one.jodi.core.extensions.contexts.TransformationNameExecutionContext;
import one.jodi.core.extensions.strategies.TransformationNameStrategy;

/**
 * This class implements the default logic to determine the transformation name
 * and implements the interface {@link TransformationNameStrategy}.
 * This logic is always executed before the custom plug-in is executed.
 * <p>
 * The class is a concrete strategy participating in the Strategy Pattern.
 */
public class TransformationNameDefaultStrategy implements TransformationNameStrategy {

    /**
     * This method determines the name of the transformation using the execution
     * context that is created for this plug-in feature.
     * <p>
     * The default strategy uses the target data store name as the basis for the
     * transformation name unless it is explicitly overridden in the
     * specification. A prefix defined in the properties is added unless the
     * target is a temporary table.
     *
     * @param explicitName contains the explicitly defined name as defined in the XML
     *                     specification. It may contain null if no transformation name
     *                     is explicitly defined
     * @param execContext  offers a set of Jodi and encapsulated ODI information to
     *                     support the decision
     */
    @Override
    public String getTransformationName(final String explicitName,
                                        final TransformationNameExecutionContext execContext) {

        String name;
        if ((explicitName != null) && (!explicitName.equals(""))) {
            name = explicitName;
        } else {
            name = execContext.getTargetDataStore().getDataStoreName();
        }
        // jkm temporary datastores now have a prefix too to make them unique
        // it is good practise to not use spaces and use uppercase prefix.
        if (execContext.getTargetDataStore().isTemporary()) {
            name = execContext.getPrefix().toUpperCase().replace(" ", "").trim().substring(0, 1) + "_" + name;
        } else {
            name = execContext.getPrefix() + name;
        }
        // end jkm
        return name;
    }
}
