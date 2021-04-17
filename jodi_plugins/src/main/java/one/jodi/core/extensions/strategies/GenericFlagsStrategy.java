package one.jodi.core.extensions.strategies;

import one.jodi.core.extensions.contexts.FlagsDataStoreExecutionContext;
import one.jodi.core.extensions.contexts.FlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.contexts.UDFlagsTargetColumnExecutionContext;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;
import one.jodi.model.extensions.TargetColumnExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Deprecated
public class GenericFlagsStrategy implements FlagsStrategy {

    private UserDefinedFlag createFlag(final String name, final Boolean value) {
        return new UserDefinedFlag() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean getValue() {
                return value;
            }
        };
    }

    private Optional<UserDefinedFlag> getUdValue(final String udName,
                                                 final Set<UserDefinedFlag> udFlags) {
        assert (udFlags != null);
        return udFlags.stream()
                .filter(u -> u.getName().equalsIgnoreCase(udName))
                .findFirst();
    }

    private Map<String, UserDefinedFlag> getSpecifiedUdValues(
            final TargetColumnExtension extension) {
        Map<String, UserDefinedFlag> flags = new HashMap<>();
        if (extension == null) {
            return flags;
        }
        if (extension.isUd1() != null) {
            flags.put("ud1", createFlag("ud1", extension.isUd1()));
        }
        if (extension.isUd2() != null) {
            flags.put("ud2", createFlag("ud2", extension.isUd2()));
        }
        if (extension.isUd3() != null) {
            flags.put("ud3", createFlag("ud3", extension.isUd3()));
        }
        if (extension.isUd4() != null) {
            flags.put("ud4", createFlag("ud4", extension.isUd4()));
        }
        if (extension.isUd5() != null) {
            flags.put("ud5", createFlag("ud5", extension.isUd5()));
        }
        if (extension.isUd6() != null) {
            flags.put("ud6", createFlag("ud6", extension.isUd6()));
        }
        if (extension.isUd7() != null) {
            flags.put("ud7", createFlag("ud7", extension.isUd7()));
        }
        if (extension.isUd8() != null) {
            flags.put("ud8", createFlag("ud8", extension.isUd8()));
        }
        if (extension.isUd9() != null) {
            flags.put("ud9", createFlag("ud9", extension.isUd9()));
        }
        if (extension.isUd10() != null) {
            flags.put("ud10", createFlag("ud10", extension.isUd10()));
        }
        return flags;
    }

    private Set<UserDefinedFlag> merge(final Map<String, UserDefinedFlag> udFlags,
                                       final Set<UserDefinedFlag> defaultFlags) {
        assert (defaultFlags != null);
        final Set<UserDefinedFlag> allUdfs = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            String name = "ud" + i;
            UserDefinedFlag ud = udFlags.get(name);
            // pick defined value or assign a false value otherwise
            if (ud != null) {
                allUdfs.add(ud);
            } else {
                Optional<UserDefinedFlag> defValue = getUdValue(name, defaultFlags);
                defValue.ifPresent(allUdfs::add);
            }
        }
        return Collections.unmodifiableSet(allUdfs);
    }

    @Override
    public Set<UserDefinedFlag> getUserDefinedFlags(
            final Set<UserDefinedFlag> defaultValues,
            final FlagsDataStoreExecutionContext tableContext,
            final UDFlagsTargetColumnExecutionContext columnContext) {
        if (columnContext == null) {
            return defaultValues;
        }
        return merge(getSpecifiedUdValues(columnContext.getTargetColumnExtension()),
                defaultValues);
    }

    @Override
    public TargetColumnFlags getTargetColumnFlags(
            final TargetColumnFlags defaultValues,
            final FlagsDataStoreExecutionContext targetDataStoreContext,
            final FlagsTargetColumnExecutionContext targetColumnContext) {

        TargetColumnFlags customValues = defaultValues;
        if (targetColumnContext == null) {
            return customValues;
        }
        final TargetColumnExtension extension =
                targetColumnContext.getTargetColumnExtension();

        customValues = new TargetColumnFlags() {
            @Override
            public Boolean isInsert() {
                return extension != null && extension.isInsert() != null ?
                        extension.isInsert() : defaultValues.isInsert();
            }

            @Override
            public Boolean isUpdate() {
                return extension != null && extension.isUpdate() != null ?
                        extension.isUpdate() : defaultValues.isUpdate();
            }

            @Override
            public Boolean isUpdateKey() {
                return (targetColumnContext.isExplicitUpdateKey() != null) ?
                        targetColumnContext.isExplicitUpdateKey() :
                        defaultValues.isUpdateKey();
            }

            @Override
            public Boolean isMandatory() {
                return (targetColumnContext.isExplicitMandatory() != null) ?
                        targetColumnContext.isExplicitMandatory() :
                        defaultValues.isMandatory();
            }

            @Override
            public Boolean useExpression() {
                return false;
            }
        };

        return customValues;
    }
}
