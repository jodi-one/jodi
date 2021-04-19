package one.jodi.etl.builder.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.util.Version;
import one.jodi.core.automapping.ColumnMappingContext;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.datastore.ModelCodeContext;
import one.jodi.core.executionlocation.ExecutionLocationContext;
import one.jodi.core.extensions.types.ExecutionLocationType;
import one.jodi.core.extensions.types.KnowledgeModuleConfiguration;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.extensions.types.UserDefinedFlag;
import one.jodi.core.folder.FolderNameContext;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.km.KnowledgeModuleContext;
import one.jodi.core.targetcolumn.FlagsContext;
import one.jodi.core.transformation.TransformationNameContext;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.builder.DeleteTransformationContext;
import one.jodi.etl.builder.EnrichingBuilder;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.KmType;
import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.MappingCommand;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.SubQuery;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.internalmodel.impl.DatasetImpl;
import one.jodi.etl.internalmodel.impl.KmTypeImpl;
import one.jodi.etl.internalmodel.impl.LookupImpl;
import one.jodi.etl.internalmodel.impl.MappingsImpl;
import one.jodi.etl.internalmodel.impl.SourceImpl;
import one.jodi.etl.internalmodel.impl.SubQueryImpl;
import one.jodi.etl.internalmodel.impl.TargetcolumnImpl;
import one.jodi.etl.internalmodel.impl.TransformationImpl;
import one.jodi.etl.internalmodel.impl.UserDefinedFlagImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Implementation of EnrichingBuilder
 */
public class EnrichingBuilderImpl implements EnrichingBuilder {

    private static final Logger LOGGER = LogManager.getLogger(EnrichingBuilderImpl.class);
    private static final String NEW_LINE = System.getProperty("line.separator");

    private static final Pattern PATTERN_UDF_NUMBER = Pattern.compile("ud(\\d+)\\z",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final String ERROR_MESSAGE_03240 = "KnowledgeModuleConfiguration can't be null.";
    private static final String ERROR_MESSAGE_08070 =
            "convertUDFs(Set<UserDefinedFlag>): Error parsing UDF number from name %s";

    private final JodiProperties properties;
    private final ColumnMappingContext columnMappingContext;
    private final ExecutionLocationContext executionLocationContext;
    private final FlagsContext flagsContext;
    private final FolderNameContext folderNameContext;
    private final JournalizingContext journalizingContext;
    private final KnowledgeModuleContext knowledgeModuleContext;
    private final ModelCodeContext modelCodeContext;
    private final TransformationNameContext transformationNameContext;
    private final ETLValidator etlValidator;
    private final ErrorWarningMessageJodi errorWarningMessages;

    private final Pattern tempNameTableMatcher;

    @Inject
    public EnrichingBuilderImpl(
            final ColumnMappingContext columnMappingContext,
            final ExecutionLocationContext executionLocationContext,
            final FlagsContext flagsContext,
            final FolderNameContext folderNameContext,
            final JournalizingContext journalizingContext,
            final KnowledgeModuleContext knowledgeModuleContext,
            final ModelCodeContext modelCodeContext,
            final TransformationNameContext transformationNameContext,
            final JodiProperties properties, final ETLValidator etlValidator,
            final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        this.columnMappingContext = columnMappingContext;
        this.executionLocationContext = executionLocationContext;
        this.flagsContext = flagsContext;
        this.folderNameContext = folderNameContext;
        this.journalizingContext = journalizingContext;
        this.knowledgeModuleContext = knowledgeModuleContext;
        this.modelCodeContext = modelCodeContext;
        this.transformationNameContext = transformationNameContext;
        this.properties = properties;
        this.etlValidator = etlValidator;
        this.errorWarningMessages = errorWarningMessages;
        this.tempNameTableMatcher = Pattern.compile(this.properties.getTemporaryInterfacesRegex());
    }


    private String getCurrentDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
    }

    @Override
    public DeleteTransformationContext createDeleteContext(
            Transformation existingTransformation, boolean isJournalizedData) {

        LOGGER.debug("------------------------------------------------");
        LOGGER.debug(" createDeleteContext.");
        LOGGER.debug("------------------------------------------------");
        final TransformationImpl transformation = new TransformationImpl();
        transformation.setExtension(existingTransformation.getExtension());
        transformation.setName(existingTransformation.getName());
        transformation.setOriginalFolderPath(existingTransformation.getOriginalFolderPath());
        MappingsImpl mappings = new MappingsImpl();
        mappings.setModel(existingTransformation.getMappings().getModel());
        mappings.setParent(transformation);
        mappings.setTargetDataStore(existingTransformation.getMappings()
                .getTargetDataStore());
        transformation.setMappings(mappings);

        // Target columns need to be added to compute folder name when target is temporary (for context)
        for (Targetcolumn tc : existingTransformation.getMappings().getTargetColumns()) {
            TargetcolumnImpl targetColumn = new TargetcolumnImpl();
            targetColumn.setName(tc.getName());
            targetColumn.setDataType(tc.getDataType());
            targetColumn.setLength(tc.getLength());
            targetColumn.setScale(tc.getScale());
            targetColumn.setExtension(tc.getExtension());
            targetColumn.setPosition(tc.getPosition());
            targetColumn.addMappingExpressions(tc.getMappingExpressions());

            targetColumn.setParent(mappings);
            mappings.addTargetcolumns(targetColumn);
        }
        transformation.setPackageSequence(existingTransformation.getPackageSequence());
        LOGGER.debug("Computing delete package sequence " + transformation.getPackageSequence());
        if (!isTemporaryTransformation(transformation.getMappings().getTargetDataStore())) {
            transformation.setTemporary(false);
            MappingsImpl mps = (MappingsImpl) transformation.getMappings();
            mps.setTargetDataStore(transformation.getMappings().getTargetDataStore());
            mps.setModel(modelCodeContext.getModelCode(transformation.getMappings()));
        } else {
            transformation.setTemporary(true);
            modelCodeContext.getModelCode(transformation.getMappings());
            folderNameContext.getFolderName(transformation, isJournalizedData);

        }
        transformation.setFolderName(folderNameContext.getFolderName(transformation, isJournalizedData));
        transformation.setName(transformationNameContext.getTransformationName(transformation));
        return new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformation.getName();
            }

            @Override
            public String getDataStoreName() {
                return transformation.getMappings().getTargetDataStore();
            }

            @Override
            public String getModel() {
                return transformation.getMappings().getModel();
            }

            @Override
            public int getPackageSequence() {
                return transformation.getPackageSequence();
            }

            @Override
            public boolean isTemporary() {
                return transformation.isTemporary();
            }

            @Override
            public String getFolderName() {
                return transformation.getFolderName();
            }

        };

    }

    private List<ExecutionLocationtypeEnum> convert(List<ExecutionLocationType> in) {
        return in.stream()
                .map(el -> ExecutionLocationtypeEnum.fromValue(el.name()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Transformation enrich(Transformation transformation, boolean isJournalizedData) {
        LOGGER.debug("------------------------------------------------");
        LOGGER.debug(transformation.getPackageSequence()
                + " enriching input model with Jodi derived information using plugins.");
        LOGGER.debug("------------------------------------------------");

        if (transformation.getFolderName() != null && transformation.getName() != null) {
            LOGGER.debug(transformation.getPackageSequence() + " transformation name, " +
                    "model and folder already set, returning without deriving");
            return transformation;
        }

        ((TransformationImpl) transformation).setTemporary(isTemporaryTransformation(
                transformation.getMappings().getTargetDataStore()));
        modelCodeContext.getModelCode(transformation.getMappings());
        folderNameContext.getFolderName(transformation, isJournalizedData);

        transformationNameContext.getTransformationName(transformation);

        etlValidator.validatePackageAssociations(transformation);

        etlValidator.validateDataset(transformation.getDatasets());

        // Set models for each source (required by ExecutionLocation plugin to
        // compute isSameModelAsTransformation and for the various KM
        // operations)
        int dataSetNumber = 0;
        for (Dataset dataset : transformation.getDatasets()) {
            boolean firstSource = true;
            for (Source source : dataset.getSources()) {
                if (firstSource) {
                    ((DatasetImpl) dataset).setName(source.getName() + "_" + dataSetNumber);
                    firstSource = false;
                }
                ((SourceImpl) source).setTemporary(isTemporaryTransformation(source.getName()));
                transformationNameContext.setSourceName(source);

                modelCodeContext.getModelCode(source);

                etlValidator.validateFilter(source);
                etlValidator.validateJoin(source);

                for (Lookup lookup : source.getLookups()) {
                    ((LookupImpl) lookup).setTemporary(isTemporaryTransformation(lookup.getLookupDataStore()));
                    transformationNameContext.setLookupName(lookup);

                    // must be executed after name is set for temp table due to
                    // required lookup in cache with derived name
                    // ((LookupImpl) lookup).setModel(modelCodeContext.getModelCode(lookup));
                    modelCodeContext.getModelCode(lookup);
                }
            }
            dataSetNumber++;
        }
        etlValidator.validateBeginAndEndMapping(transformation);
        // depends on model code
        knowledgeModuleContext.getIKMConfig(transformation);
        // ((MappingsImpl) transformation.getMappings()).setIkm(generateKMType(ikm));

        knowledgeModuleContext.getStagingModel(transformation);
        // ((MappingsImpl) transformation.getMappings()).setStagingModel(stagingModel);

        for (Dataset dataset : transformation.getDatasets()) {
            for (Source source : dataset.getSources()) {
                SourceImpl srcAsImpl = (SourceImpl) source;
                executionLocationContext.getFilterExecutionLocation(srcAsImpl);
                executionLocationContext.getJoinExecutionLocation(srcAsImpl);

                if (isTemporaryTransformation(srcAsImpl.getName())) {
                    srcAsImpl.setTemporary(true);
                }

                LOGGER.debug("Journalized in enrichment:" + srcAsImpl.isJournalized());

                knowledgeModuleContext.getLKMConfig(srcAsImpl, "");

                srcAsImpl.setSubSelect(srcAsImpl.isSubSelect());
                etlValidator.validateSourceDataStore(srcAsImpl);
                etlValidator.validateSourceDataStoreName(srcAsImpl);

                srcAsImpl.setJournalizedType(journalizingContext.isJournalizedSource(srcAsImpl));
                if (journalizingContext.isJournalizedSource(srcAsImpl)) {
                    journalizingContext.getJournalizingConfiguration().stream()
                            .filter(journalizingConfig -> journalizingConfig
                                    .getModelCode()
                                    .equals(srcAsImpl.getModel()))
                            .forEach(journalizingConfig -> srcAsImpl
                                    .setSubscribers(journalizingConfig.getSubscribers()));
                }
                for (Lookup lookup : srcAsImpl.getLookups()) {
                    LookupImpl lkpAsImpl = (LookupImpl) lookup;
                    if (isJournalizedData) {
                        lkpAsImpl.setJournalized(journalizingContext.isJournalizedLookup(lkpAsImpl));
                        if (journalizingContext.isJournalizedLookup(lkpAsImpl)) {
                            journalizingContext.getJournalizingConfiguration()
                                    .stream()
                                    .filter(journalizingConfig -> journalizingConfig
                                            .getModelCode()
                                            .equals(lkpAsImpl.getModel()))
                                    .forEach(journalizingConfig -> lkpAsImpl.setSubscribers(
                                            journalizingConfig.getSubscribers()));
                        }
                    }
                    lkpAsImpl.setTemporary(isTemporaryTransformation(lkpAsImpl.getLookupDataStore()));

                    executionLocationContext.getLookupExecutionLocation(lkpAsImpl);
                    lkpAsImpl.setSubSelect(lkpAsImpl.isSubSelect());
                    etlValidator.validateLookupJoin(lkpAsImpl);
                    etlValidator.validateLookup(lkpAsImpl);
                    etlValidator.validateLookupName(lkpAsImpl);
                    etlValidator.validateNoMatchRows(lkpAsImpl);
                }


                srcAsImpl.getFlows().stream()
                        .filter(c -> c instanceof SubQuery)
                        .map(SubQueryImpl.class::cast)
                        .peek(modelCodeContext::getModelCode)
                        .peek(etlValidator::validateExecutionLocation)
                        .peek(executionLocationContext::getSubQueryExecutionLocation)
                        .peek(sq -> sq.setTemporary(isTemporaryTransformation(sq.getFilterSource())))
                        .forEach(etlValidator::validateFlow);
                srcAsImpl.getFlows().stream()
                        .filter(c -> !(c instanceof SubQuery))
                        .forEach(etlValidator::validateFlow);
            }
        }

        knowledgeModuleContext.getCKMConfig(transformation);

        // mapping expressions are set in the Transformation object
        columnMappingContext.getMappings(transformation);

        // Now process flags
        int position = 0;
        for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
            position++;
            TargetcolumnImpl tcAsImpl = (TargetcolumnImpl) tc;
            TargetColumnFlags flags = flagsContext.getTargetColumnFlags(tcAsImpl);
            tcAsImpl.setMandatory(flags.isMandatory());
            tcAsImpl.setInsert(flags.isInsert());
            tcAsImpl.setUpdate(flags.isUpdate());
            tcAsImpl.setUpdateKey(flags.isUpdateKey());
            if (flags.isUpdateKey() == null) {
                LOGGER.error(tcAsImpl.getParent().getTargetDataStore() + "." + tcAsImpl.getName() + " is " + flags.isUpdateKey());
            }
            ((TransformationImpl) transformation).setUseExpressions(flags.useExpression());

            Set<UserDefinedFlag> userDefinedFlags = flagsContext.getUserDefinedFlags(tcAsImpl);
            if (userDefinedFlags != null) {
                tcAsImpl.setUserDefinedFlags(convertUDFs(userDefinedFlags));
            }
            tcAsImpl.setPosition(position);
            etlValidator.validateTargetColumn(tcAsImpl);
        }
        etlValidator.validateTargetColumns(transformation.getMappings());
        // Compute Execution Location for each target column's mapping expressions
        for (Targetcolumn targetColumn : transformation.getMappings().getTargetColumns()) {

            List<ExecutionLocationType> executionLocations = executionLocationContext
                    .getTargetColumnExecutionLocation(targetColumn);
            ((TargetcolumnImpl) targetColumn).setExecutionLocations(convert(executionLocations));
        }

        etlValidator.validateJournalized(transformation);

        transformation.getDatasets().stream()
                .flatMap(ds -> ds.getSources().stream())
                .forEach(src -> {
                    etlValidator.validateFilterEnriched(src);
                    etlValidator.validateJoinEnriched(src);

                    src.getLookups().forEach(etlValidator::validateJoinEnriched);
                });

        for (Dataset dataset : transformation.getDatasets()) {
            for (Source source : dataset.getSources()) {
                etlValidator.validateFilterEnriched(source);
                etlValidator.validateJoinEnriched(source);

                source.getLookups().forEach(etlValidator::validateJoinEnriched);
            }
        }

        // add build information
        StringBuilder description = new StringBuilder();
        if (transformation.getComments() != null) {
            description.append(transformation.getComments());
        }
        if (properties.includeDetails()) {
            if (transformation.getComments() != null
                    && !transformation.getComments().trim().equals("")) {
                description.append(NEW_LINE);
                description.append(NEW_LINE);
            }
            description.append("Bulk creation operation for ");
            description.append(transformation.getMappings().getTargetDataStore());
            description.append(" with sequence number ");
            description.append(transformation.getPackageSequence());
            description.append(".  Imported by ");
            description.append(System.getProperty("user.name"));
            description.append(" at ");
            description.append(getCurrentDate());
            description.append(NEW_LINE);
            description.append("Created by Jodi version ");
            description.append(Version.getProductVersion());
            description.append(" with build date ");
            description.append(Version.getBuildDate());
            description.append(" ");
            description.append(Version.getBuildTime());
        }
        ((TransformationImpl) transformation).setComments(description.toString());

        //logger.info("Transformation: "+ transformation.getName() + " : "+ transformation.getBeginMappingCommand());
        if (transformation.getBeginMappingCommand() != null) {
            ((TransformationImpl) transformation).setBeginMappingCommand(getMappingCmd(transformation.getBeginMappingCommand()));
        }
        if (transformation.getEndMappingCommand() != null) {
            ((TransformationImpl) transformation).setEndMappingCommand(getMappingCmd(transformation.getEndMappingCommand()));
        }
        if (transformation.isAsynchronous()) {
            ((TransformationImpl) transformation).setAsynchronous(transformation.isAsynchronous());
        }
        return transformation;
    }

    private MappingCommand getMappingCmd(final MappingCommand beginOrEndMappingCommand) {
        return new MappingCommand() {

            @Override
            public String getText() {
                return beginOrEndMappingCommand.getText().trim();
            }

            @Override
            public String getTechnology() {
                return beginOrEndMappingCommand.getTechnology();
            }

            @Override
            public String getModel() {
                return modelCodeContext.getModelCode(beginOrEndMappingCommand.getModel());
            }
        };
    }

    @SuppressWarnings("unused")
    private KmType generateKMType(KnowledgeModuleConfiguration kmc) {
        if (kmc == null) {
            String msg = errorWarningMessages.formatMessage(3240,
                    ERROR_MESSAGE_03240, this.getClass());
            LOGGER.error(msg);
            throw new UnRecoverableException(msg);
        }
        if (kmc == KnowledgeModuleConfiguration.Null) {
            return null;
        }

        KmTypeImpl type = new KmTypeImpl();
        type.setName(kmc.getName());
        for (String k : kmc.getOptionKeys()) {
            type.addOption(k, kmc.getOptionValue(k) + "");
        }
        return type;
    }

    private Set<one.jodi.etl.internalmodel.UserDefinedFlag> convertUDFs(Set<UserDefinedFlag> externalFlags) {
        Set<one.jodi.etl.internalmodel.UserDefinedFlag> result = new HashSet<>(externalFlags.size());
        for (UserDefinedFlag udf : externalFlags) {
            try {
                Matcher regexMatcher = PATTERN_UDF_NUMBER.matcher(udf.getName());
                if (regexMatcher.matches()) {
                    int udfNumber = Integer.parseInt(regexMatcher.group(1));
                    result.add(new UserDefinedFlagImpl(udfNumber, udf.getValue()));
                }
            } catch (NumberFormatException | PatternSyntaxException e) {
                String msg = errorWarningMessages.formatMessage(8070, ERROR_MESSAGE_08070, this.getClass(), udf.getName());
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
                LOGGER.error(msg);
            }
        }
        return result;
    }

    @Override
    public boolean isTemporaryTransformation(String tableName) {
        return tempNameTableMatcher.matcher(tableName).find();
    }
}
