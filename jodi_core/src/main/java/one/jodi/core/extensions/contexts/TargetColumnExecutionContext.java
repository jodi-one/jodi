package one.jodi.core.extensions.contexts;

import one.jodi.core.executionlocation.ExecutionLocationType;
import one.jodi.model.extensions.TargetColumnExtension;

/**
 * This interface is used to provide shared contextual information for execution
 * context objects that have the scope of a mappings target column.
 */
public interface TargetColumnExecutionContext {

    /**
     * Indicates if a target column was explicitly mapped in the transformation
     * XML specification.
     *
     * @return <code>true</code> in case that the mapping is explicitly defined;
     * <code>false</code> if the mapping is defined through the
     * auto-mapping feature
     */
    public boolean isExplicitlyMapped();

    /**
     * Get the explicit definition of the mandatory (check not null) field for the target column.  Returns null when not explicitly defined.
     *
     * @return mandatory
     */
    public Boolean isExplicitMandatory();

    /**
     * Get the explicit definition of the update field for the target column.  Returns null when not explicity defined.
     *
     * @return update
     */
    public Boolean isExplicitUpdateKey();

    /**
     * Gets the name of the target column for which this context is created.
     *
     * @return the column name
     */
    public String getTargetColumnName();

    /**
     * Gets target column extension that is associated with the XML
     * specification target column element.
     *
     * @return Extension element that may be customized as a consequence of XSD
     * customizations.
     */
    public TargetColumnExtension getTargetColumnExtension();

    /**
     * @return Indicating an analytical function.
     */
    public boolean isAnalyticalFunction();

    ExecutionLocationType getExplicitTargetColumnExecutionLocation();
}