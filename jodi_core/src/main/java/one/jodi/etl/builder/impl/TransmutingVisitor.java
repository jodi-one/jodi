package one.jodi.etl.builder.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.core.annotations.InterfacePrefix;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.executionlocation.ExecutionLocationType;
import one.jodi.core.model.*;
import one.jodi.core.model.impl.DatasetImpl;
import one.jodi.core.model.impl.DatasetsImpl;
import one.jodi.core.model.impl.KmTypeImpl;
import one.jodi.core.model.impl.LookupsImpl;
import one.jodi.core.model.visitors.BaseVisitor;
import one.jodi.etl.internalmodel.GroupComparisonEnum;
import one.jodi.etl.internalmodel.MappingCommand;
import one.jodi.etl.internalmodel.SubQuery;
import one.jodi.etl.internalmodel.UserDefinedFlag;
import one.jodi.etl.internalmodel.impl.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * This class uses the visitor pattern where possible to transform the external model into the Jodi internal model.
 * <p>
 * As the visitor only receives events the derivation of model and KM is computed using direct access to the external hierarchy.
 */
public class TransmutingVisitor extends BaseVisitor {

    private static final String ERROR_MESSAGE_02090 = "Dataset index not found";
    private static final Logger logger = LogManager.getLogger(TransmutingVisitor.class);
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final Pattern tempNameTableMatcher;
    private final String prefix;
    private final JodiProperties properties;
    TransformationImpl internalTransformation = null;
    /**
     * Used for building objects that roll up several types in the external model, each of which is handled in a separate visitor method.
     */
    private List<TargetcolumnImpl> targetcolumns =
            new LinkedList<>();
    private MappingsImpl mappings = null;
    private List<LookupImpl> lookups =
            new LinkedList<>();
    private List<SourceImpl> sources =
            new LinkedList<>();
    private List<one.jodi.etl.internalmodel.impl.DatasetImpl> datasets =
            new LinkedList<>();
    private List<FlowImpl> flows =
            new LinkedList<>();

    public TransmutingVisitor(final ErrorWarningMessageJodi errorWarningMessages,
                              final @InterfacePrefix String prefix,
                              final JodiProperties properties) {
        this.errorWarningMessages = errorWarningMessages;
        this.prefix = prefix;
        this.properties = properties;
        if (this.properties != null) {
            this.tempNameTableMatcher = Pattern.compile(this.properties.getTemporaryInterfacesRegex());
        } else {
            this.tempNameTableMatcher = Pattern.compile("(_S)([0-9]{1,}){1,1}");
        }
    }

    public TransformationImpl getTransformation(int packageSequence) {
        internalTransformation.setPackageSequence(packageSequence);
        return internalTransformation;
    }

    @Override
    public void visit(final Transformation externalTransformation) {
        internalTransformation = new TransformationImpl();
        internalTransformation.setComments(externalTransformation.getComments());
        internalTransformation.setName(externalTransformation.getName());
        internalTransformation.setOriginalFolderPath(externalTransformation.getFolderName());
        internalTransformation.setPackageList(externalTransformation.getPackageList());
        internalTransformation.setExtension(externalTransformation.getExtension());
        if (externalTransformation.getBeginCommand() != null && externalTransformation.getBeginCommand().getText().length() > 0) {
            MappingCommand beginMapCmd = new MappingCommand() {

                @Override
                public String getText() {
                    StringBuilder text = new StringBuilder();
                    for (String line : externalTransformation.getBeginCommand().getText().split("[\\r\\n]+")) {
                        text.append(line + System.getProperty("line.separator"));
                    }
                    text.append(System.getProperty("line.separator"));
                    return text.toString();
                }

                @Override
                public String getTechnology() {
                    return externalTransformation.getBeginCommand().getTechnology()
                            .name();
                }

                @Override
                public String getModel() {
                    return externalTransformation.getBeginCommand().getLocation();
                }
            };
            internalTransformation.setBeginMappingCommand(beginMapCmd);
        }
        if (externalTransformation.getEndCommand() != null && externalTransformation.getEndCommand().getText().length() > 0) {
            MappingCommand endMapCmd = new MappingCommand() {

                @Override
                public String getText() {
                    StringBuilder text = new StringBuilder();
                    for (String line : externalTransformation.getEndCommand().getText().split("[\\r\\n]+")) {
                        text.append(line + System.getProperty("line.separator"));
                    }
                    text.append(System.getProperty("line.separator"));
                    return text.toString();
                }

                @Override
                public String getTechnology() {
                    return externalTransformation.getEndCommand().getTechnology()
                            .name();
                }

                @Override
                public String getModel() {
                    return externalTransformation.getEndCommand().getLocation();
                }
            };
            internalTransformation.setEndMappingCommand(endMapCmd);
        }

        boolean first = true;
        for (one.jodi.etl.internalmodel.impl.DatasetImpl dataset : datasets) {
            dataset.setParent(internalTransformation);
            internalTransformation.addDataset(dataset);
            if (first) {
                first = false;
                if (dataset.getSetOperator() == null) {
                    dataset.setSetOperator(one.jodi.etl.internalmodel.SetOperatorTypeEnum.NOT_DEFINED);
                }
            } else {
                if (dataset.getSetOperator() == null) {
                    dataset.setSetOperator(one.jodi.etl.internalmodel.SetOperatorTypeEnum.UNION_ALL);
                }
            }
        }
        datasets.clear();

        internalTransformation.setMappings(mappings);
        if (mappings != null) {
            mappings.setParent(internalTransformation);
        }
        mappings = null;
        // if asynchronous is null then set to false.
        internalTransformation.setAsynchronous(externalTransformation.isAsynchronous() == null ? false : externalTransformation.isAsynchronous());
    }


    @Override
    public void visit(Dataset external) {
        one.jodi.etl.internalmodel.impl.DatasetImpl internal = new one.jodi.etl.internalmodel.impl.DatasetImpl();
        internal.setName(external.getName());
        internal.setSetOperator(transmute(external.getSetOperator()));

        for (SourceImpl source : sources) {
            source.setParent(internal);
            internal.addSource(source);
        }
        sources.clear();

        datasets.add(internal);
    }


    @Override
    public void visit(Source external) {
        SourceImpl internal = new SourceImpl();
        internal.setAlias(external.getAlias() != null && external.getAlias().length() > 0 ? external.getAlias() : external.getName());
        internal.setName(external.getName());
        internal.setModel(external.getModel());
        internal.setFilter(external.getFilter());
        internal.setFilterExecutionLocation(transmute(external.getFilterExecutionLocation()));
        internal.setJoin(external.getJoin());
        internal.setJoinExecutionLocation(transmute(external.getJoinExecutionLocation()));
        internal.setJoinType(transmute(external.getJoinType()));
        internal.setSubSelect(external.isSubSelect() != null ? external.isSubSelect() : false);
        internal.setJournalizedType(transmute(external.isJournalized()));
        internal.setExtension(external.getExtension());


        // Set model using over-ride rules
        if (external.getModel() != null) {
            internal.setModel(external.getModel());
        } else if (((Dataset) external.getParent()).getModel() != null) {
            internal.setModel(((Dataset) external.getParent()).getModel());
        } else if (((Datasets) external.getParent().getParent()).getModel() != null) {
            internal.setModel(((Datasets) external.getParent().getParent()).getModel());
        }

        // Set LKM
        if (((DatasetImpl) external.getParent()).getLkm() != null) {
            internal.setLkm(transmute((KmTypeImpl) ((DatasetImpl) external.getParent()).getLkm()));
        } else if (((DatasetsImpl) external.getParent().getParent()).getLkm() != null) {
            internal.setLkm(transmute((KmTypeImpl) ((DatasetsImpl) external.getParent().getParent()).getLkm()));
        }
        // Source LKM always overrides LKM from dataset
        if (external.getLkm() != null) {
            internal.setLkm(transmute((KmTypeImpl) external.getLkm()));
        }

        for (LookupImpl lookup : lookups) {
            lookup.setParent(internal);
            internal.addLookup(lookup);
        }

        for (FlowImpl flow : flows) {
            flow.setParent(internal);
            internal.add(flow);
        }

        lookups.clear();
        flows.clear();
        sources.add(internal);
    }


    @Override
    public void visit(Lookup external) {
        LookupImpl internal = new LookupImpl();
        internal.setAlias(external.getAlias() != null && external.getAlias().length() > 0 ? external.getAlias() : external.getLookupDataStore());
        internal.setJoin(external.getJoin());
        internal.setJoinExecutionLocation(transmute(external.getJoinExecutionLocation()));
        internal.setLookupDatastore(external.getLookupDataStore());
        internal.setLookupType(transmute(external.getLookupType()));
        internal.setSubSelect(external.isSubSelect());

        internal.setJournalized(external.isJournalized());

        if (external.getModel() != null) {
            internal.setModel(external.getModel());
        } else if (((LookupsImpl) external.getParent()).getModel() != null) {
            internal.setModel(((LookupsImpl) external.getParent()).getModel());
        }

        if (external.getNoMatchRow() != null && external.getNoMatchRow().getClass() != null) {
            external.getNoMatchRow().getColumn().forEach(c -> {
                internal.getDefaultRowColumns().put(c.getName(), c.getValue());
            });

        }

        lookups.add(0, internal);
    }


    @Override
    public void visit(Mappings external) {
        MappingsImpl internal = new MappingsImpl();

        internal.setModel(external.getModel());
        internal.setDistinct(external.isDistinct());
        internal.setTargetDataStore(external.getTargetDataStore());
        internal.setExtension(external.getExtension());
        internal.setStagingModel(external.getStagingModel());

        for (TargetcolumnImpl targetcolumn : targetcolumns) {
            targetcolumn.setParent(internal);
            internal.addTargetcolumns(targetcolumn);
        }

        targetcolumns.clear();

        internal.setIkm(transmute((KmTypeImpl) external.getIkm()));
        internal.setCkm(transmute((KmTypeImpl) external.getCkm()));

        mappings = internal;
    }


    private Set<UserDefinedFlag> getUdFlags(final Targetcolumn column) {
        Set<UserDefinedFlag> udFlags = new HashSet<>();
        if (column.isUd1() != null) {
            udFlags.add(new UserDefinedFlagImpl(1, column.isUd1()));
        }
        ;
        if (column.isUd2() != null) {
            udFlags.add(new UserDefinedFlagImpl(2, column.isUd2()));
        }
        ;
        if (column.isUd3() != null) {
            udFlags.add(new UserDefinedFlagImpl(3, column.isUd3()));
        }
        ;
        if (column.isUd4() != null) {
            udFlags.add(new UserDefinedFlagImpl(4, column.isUd4()));
        }
        ;
        if (column.isUd5() != null) {
            udFlags.add(new UserDefinedFlagImpl(5, column.isUd5()));
        }
        ;
        if (column.isUd6() != null) {
            udFlags.add(new UserDefinedFlagImpl(6, column.isUd6()));
        }
        ;
        if (column.isUd7() != null) {
            udFlags.add(new UserDefinedFlagImpl(7, column.isUd7()));
        }
        ;
        if (column.isUd8() != null) {
            udFlags.add(new UserDefinedFlagImpl(8, column.isUd8()));
        }
        ;
        if (column.isUd9() != null) {
            udFlags.add(new UserDefinedFlagImpl(9, column.isUd9()));
        }
        ;
        if (column.isUd10() != null) {
            udFlags.add(new UserDefinedFlagImpl(10, column.isUd10()));
        }
        ;
        return udFlags;
    }

    @Override
    public void visit(Targetcolumn external) {
        TargetcolumnImpl internal = new TargetcolumnImpl();
        internal.setName(external.getName());
        internal.setUpdateKey(external.isKey());
        internal.setMandatory(external.isMandatory());
        internal.setExplicitMandatory(external.isMandatory());
        internal.setExplicitUpdateKey(external.isKey());
        internal.setExplicitUserDefinedFlags(getUdFlags(external));
        internal.setExtension(external.getExtension());
        if (external.isUpdate() != null) {
            internal.setUpdate(external.isUpdate());
        }
        if (external.isInsert() != null) {
            internal.setInsert(external.isInsert());
        }
        if (external.getProperties() != null) {
            internal.setDataType(external.getProperties().getDataType());
            internal.setScale(external.getProperties().getScale() != null ? external.getProperties().getScale() : 0);
            internal.setLength(external.getProperties().getLength() != null ? external.getProperties().getLength() : 0);
        }

        if (external.getMappingExpressions() != null) {
            internal.addMappingExpressions(external.getMappingExpressions().getExpression());
        }
        if (external.getExecutionLocation() != null) {
            internal.setTargetcolumnExplicitExecutionLocation(ExecutionLocationType.valueOf(external.getExecutionLocation().name()));
        }
        targetcolumns.add(internal);
    }


    private one.jodi.etl.internalmodel.ExecutionLocationtypeEnum transmute(ExecutionLocationtypeEnum external) {
        return external != null ? one.jodi.etl.internalmodel.ExecutionLocationtypeEnum.fromValue(external.name()) : null;
    }


    private one.jodi.etl.internalmodel.SetOperatorTypeEnum transmute(SetOperatorTypeEnum external) {
        return external != null ? one.jodi.etl.internalmodel.SetOperatorTypeEnum.fromValue(external.name()) : null;
    }


    private one.jodi.etl.internalmodel.LookupTypeEnum transmute(LookupTypeEnum external) {
        return external != null ? one.jodi.etl.internalmodel.LookupTypeEnum.fromValue(external.value()) : null;
    }


    private one.jodi.etl.internalmodel.JoinTypeEnum transmute(JoinTypeEnum external) {
        return external != null ? one.jodi.etl.internalmodel.JoinTypeEnum.fromValue(external.name().replace("_", " ")) : null;
    }

    private one.jodi.etl.internalmodel.impl.KmTypeImpl transmute(KmTypeImpl external) {

        if (external == null) return null;

        one.jodi.etl.internalmodel.impl.KmTypeImpl internal = new one.jodi.etl.internalmodel.impl.KmTypeImpl();
        internal.setName(external.getCode());
        if (external.getKmOptions() != null && external.getKmOptions().getKmOption() != null) {
            for (KmOption option : external.getKmOptions().getKmOption()) {
                internal.addOption(option.getName(), option.getValue());
            }
        }

        return internal;
    }

    private boolean transmute(boolean external) {
        return external;
    }

    @SuppressWarnings(value = {"unused"})
    private String deriveModel(Common common) {

        Method method;
        String model;
        try {
            method = common.getClass().getMethod("getModel");
            model = (String) method.invoke(common);

        } catch (NoSuchMethodException | InvocationTargetException | IllegalArgumentException | IllegalAccessException | SecurityException e) {
            model = null;
        }
        return (model != null) ? model : deriveModel(common.getParent());

    }


    @SuppressWarnings("unused")
    private int getDatasetIndex(one.jodi.etl.internalmodel.Dataset dataset) {
        List<one.jodi.etl.internalmodel.Dataset> datasets = dataset.getParent().getDatasets();
        for (int i = 0; i < datasets.size(); i++) {
            if (dataset == datasets.get(i)) {
                return i;
            }
        }

        String msg = errorWarningMessages.formatMessage(2090, ERROR_MESSAGE_02090, this.getClass());
        errorWarningMessages.addMessage(dataset.getParent().getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
        logger.error(msg);
        throw new RuntimeException(msg);
    }

    @Override
    public void visit(PivotType external) {
        int index = 0; //((Source) external.getParent()).getFlows().getFlow().indexOf(external);

        PivotImpl internal = new PivotImpl(
                external.getName(), index, external.getRowLocator(),
                transmute(external.getAggregateFunction()));

        for (ColumnType externalColumn : external.getColumn()) {
            OutputAttributeImpl internalAttr = new OutputAttributeImpl();
            internalAttr.setName(externalColumn.getName());
            for (ColumnType.Expression expression : externalColumn.getExpression()) {
                internalAttr.getExpressions().put(expression.getValueSelector(), expression.getValue());
            }
            internal.add(internalAttr);
        }
        flows.add(internal);
    }

    @Override
    public void visit(UnPivotType external) {
        int index = 0; //((Source) external.getParent()).getFlows().getFlow().indexOf(external);

        UnPivotImpl internal = new UnPivotImpl(
                external.getName(), index, external.getRowLocator(), external.isIncludeNulls());

        for (ColumnType externalColumn : external.getColumn()) {
            OutputAttributeImpl internalAttr = new OutputAttributeImpl();
            internalAttr.setName(externalColumn.getName());
            for (ColumnType.Expression expression : externalColumn.getExpression()) {
                internalAttr.getExpressions().put(expression.getValueSelector(), expression.getValue());
            }
            internal.add(internalAttr);
        }
        flows.add(internal);
    }

    one.jodi.etl.internalmodel.AggregateFunctionEnum transmute(AggregateFunctionEnum external) {
        return one.jodi.etl.internalmodel.AggregateFunctionEnum.fromValue(external.name());
    }

    @Override
    public void visit(SubQueryType external) {
        int index = 0; //((Source) external.getParent()).getFlows().getFlow().indexOf(external);

        String filterSource = isTemporaryTransformation(external.getFilterSource()) ? prefix.substring(0, 1) + "_" + external.getFilterSource() : external.getFilterSource();

        SubQueryImpl internal = new SubQueryImpl(
                index,
                external.getName(),
                filterSource,
                external.getModel(),
                transmute(external.getExecutionLocation()),
                transmuteRole(external.getRole()),
                external.getCondition(),
                transmute(external.getGroupComparison()));

        for (SubQueryType.Column externalColumn : external.getColumn()) {
            OutputAttributeImpl internalAttr = new OutputAttributeImpl();
            internalAttr.setName(externalColumn.getName());
            for (SubQueryType.Column.Expression expression : externalColumn.getExpression()) {
                SubQuery.ExpressionSource expressionSource = expression.isIsFilter() ? SubQuery.ExpressionSource.FILTER
                        : SubQuery.ExpressionSource.DRIVER;
                internalAttr.getExpressions().put(expressionSource.name(), expression.getValue());
            }
            internal.add(internalAttr);
        }
        flows.add(internal);

    }


    one.jodi.etl.internalmodel.RoleEnum transmuteRole(RoleEnum external) {
        return one.jodi.etl.internalmodel.RoleEnum.fromValue(external.value());
    }

    GroupComparisonEnum transmute(GroupComparisonConditionEnum external) {
        return GroupComparisonEnum.fromValue(external.name());
    }

    private boolean isTemporaryTransformation(String tableName) {
        return tempNameTableMatcher.matcher(tableName).find();
    }
}
