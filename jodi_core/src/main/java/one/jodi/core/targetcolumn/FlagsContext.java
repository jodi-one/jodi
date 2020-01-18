package one.jodi.core.targetcolumn;

import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Targetcolumn;

import java.util.Map;
import java.util.Set;

/**
 * The Flags context interface which allows to define insert, update, insert
 * key, mandatory and user-defined flags.
 *
 */
public interface FlagsContext {

    /**
     * Determines user-defined flags for each column in the target data store.
     *
     * @param mappings object representing the mapping definitions of the XML
     *                 transformation specification
     * @return set of user-defined flags for all columns keyed by column name
     */
    Map<String, Set<UserDefinedFlag>> getUserDefinedFlags(Mappings mappings);

    Set<UserDefinedFlag> getUserDefinedFlags(Targetcolumn targetColumn);

    /**
     * Determines target column flags for each column in the target data store.
     *
     * @param mappings object representing the mapping definitions of the XML
     *                 transformation specification
     * @return target column flags for all columns keyed by column name
     */
    Map<String, TargetColumnFlags> getTargetColumnFlags(Mappings mappings);

    TargetColumnFlags getTargetColumnFlags(Targetcolumn targetColumn);
}
