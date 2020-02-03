package one.jodi.core.targetcolumn.impl;

import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.FlagsDataStoreExecutionContext;
import one.jodi.core.extensions.contexts.FlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.contexts.UDFlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.strategies.FlagsStrategy;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;

import java.util.*;

/**
 * This class implements default user defined flag behavior. The current default
 * behavior is to return an empty collection.
 */
public class FlagsDefaultStrategy implements FlagsStrategy {

    public FlagsDefaultStrategy() {

    }

    /**
     * Gets the default value passed into the strategy, if it is set.
     *
     * @param udName  the target
     * @param udFlags the defaults
     * @return the default value
     */
    private Optional<UserDefinedFlag> getUdValue(final String udName,
                                                 final Set<UserDefinedFlag> udFlags) {
        Optional<UserDefinedFlag> result = Optional.empty();
        if (udFlags != null) {
            result = udFlags.stream()
                    .filter(u -> u.getName().equalsIgnoreCase(udName))
                    .findFirst();
        }
        return result;
    }

    private Set<UserDefinedFlag> getAllUdValues(final Set<UserDefinedFlag> udFlags) {
        final Set<UserDefinedFlag> allUdfs = new HashSet<>(10);
        for (int i = 1; i <= 10; i++) {
            String name = "ud" + i;
            Optional<UserDefinedFlag> ud = getUdValue(name, udFlags);
            // pick defined value or assign a false value otherwise
            if (ud.isPresent()) {
                allUdfs.add(ud.get());
            }
        }
        return Collections.unmodifiableSet(allUdfs);
    }


    /**
     * Default implementation of the user-defined flag strategy returns an empty
     * set as no custom flags are needed with the used default KMs.
     */
    public Set<UserDefinedFlag> getUserDefinedFlags(
            Set<UserDefinedFlag> defaultValues,
            FlagsDataStoreExecutionContext tableContext,
            UDFlagsTargetColumnExecutionContext columnContext) {
        return getAllUdValues(columnContext.getUserDefinedFlags());
    }

    /**
     * Currently only the IKM Incremental Update Knowledge module
     * requires or considers the update flag. Consequently, it can be ignored
     * otherwise. We simply check of the KM name contains the substring 'MERGE'
     * to determine if we allow an update flag to be set.
     */
    private boolean requiresUpdateKey(String kmName) {

        boolean requires = false;
        // detects IKM Incremental Update (Merge or standard)
        if (kmName.toLowerCase().contains("incremental")) {
            requires = true;
        }
        return requires;
    }

    private TargetColumnFlags createInsertUpdateFlag(final boolean isInsert,
                                                     final boolean isUpdate,
                                                     final boolean isUpdateKey,
                                                     final boolean isMandatory,
                                                     final boolean useExpressions) {
        TargetColumnFlags defaultFlags = new TargetColumnFlags() {
            @Override
            public Boolean isInsert() {
                return isInsert;
            }

            @Override
            public Boolean isUpdate() {
                return isUpdate;
            }

            @Override
            public Boolean isUpdateKey() {
                return isUpdateKey;
            }

            @Override
            public Boolean isMandatory() {
                return isMandatory;
            }

            @Override
            public Boolean useExpression() {
                return useExpressions;
            }
        };
        return defaultFlags;
    }

    /**
     * The default implementation of the insert and update flags strategy
     * considers explicitly defined 'mandatory' option and update key. An
     * explicitly defined update key will unset the update flag and set the
     * mandatory flag.
     * <p>
     * Update flags are unset for select columns that participate in SCD type 2
     * operations.
     */
    @Override
    public TargetColumnFlags getTargetColumnFlags(
            TargetColumnFlags explicitFlags,
            FlagsDataStoreExecutionContext tableContext,
            FlagsTargetColumnExecutionContext columnContext) {

        // default values for additional target row columns
        boolean isInsert = true;
        boolean isUpdate = true;
        boolean isUpdateKey = false;
        boolean isMandatory = columnContext.hasNotNullConstraint();
        boolean useExpressions = true;
        // handle explicit override of mandatory and key flags
        Map<String, PropertyValueHolder> properties = tableContext.getProperties();
        PropertyValueHolder valueHolder = properties.get(tableContext.getIKMCode() + ".name");
        String kmName = valueHolder != null ? valueHolder.getString() : null;
        if (explicitFlags != null) {
            if (explicitFlags.isMandatory() != null) {
                isMandatory = explicitFlags.isMandatory();
            }
            // if update keys is set, check if key is actually needed by the IKM
            if ((explicitFlags.isUpdateKey() != null) && (explicitFlags.isUpdateKey()) &&
                    (requiresUpdateKey(kmName))) {
                isUpdateKey = true;
                isMandatory = true;
                isUpdate = false;
            }
        }

        PropertyValueHolder insertDt = properties.get(JodiConstants.W_INSERT_DT);
        PropertyValueHolder rowId = properties.get(JodiConstants.ROW_WID);
        // determine if column based on name should not be updated
        if ((insertDt != null && columnContext.getTargetColumnName().equalsIgnoreCase(insertDt.getString())) ||
                (rowId != null && columnContext.getTargetColumnName().equalsIgnoreCase(rowId.getString()))) {
            isUpdate = false;
        }

        if (properties.get("odi12.suppressExpression") == null || properties.get("odi12.suppressExpression").getString() == null ||
                properties.get("odi12.suppressExpression").getString().equals("false")) {
            useExpressions = true;
        } else {
            useExpressions = false;
        }

        isInsert = overrideExplictValuesForInsert(explicitFlags, tableContext, columnContext, isInsert);
        isUpdate = overrideExplictValuesForUpdate(explicitFlags, tableContext, columnContext, isUpdate);

        return createInsertUpdateFlag(isInsert, isUpdate, isUpdateKey, isMandatory, useExpressions);
    }

    private boolean overrideExplictValuesForUpdate(TargetColumnFlags explicitFlags,
                                                   FlagsDataStoreExecutionContext tableContext, FlagsTargetColumnExecutionContext columnContext,
                                                   boolean isUpdate) {
        if (explicitFlags == null) {
            return isUpdate;
        }
        return explicitFlags.isUpdate() == null ? isUpdate : explicitFlags.isUpdate();
    }

    private boolean overrideExplictValuesForInsert(TargetColumnFlags explicitFlags,
                                                   FlagsDataStoreExecutionContext tableContext, FlagsTargetColumnExecutionContext columnContext,
                                                   boolean isInsert) {
        if (explicitFlags == null) {
            return isInsert;
        }
        return explicitFlags.isInsert() == null ? isInsert : explicitFlags.isInsert();
    }

}
