package one.jodi.action;

import com.google.inject.Inject;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.bootstrap.UsageException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.util.StringUtils;
import one.jodi.core.service.TableService;
import one.jodi.etl.service.table.TableDefaultBehaviors;
import one.jodi.etl.service.table.TableServiceProvider;

import java.util.List;


/**
 * An {@link one.jodi.bootstrap.RunConfig$ActionRunner} implementation
 * that invokes the {@link TableServiceProvider#alterSCDTables()} method.
 *
 */
public class AlterSCDTablesActionRunner implements ActionRunner {
    private final static String ERROR_MESSAGE_01010 = "Could not delete interfaces,\n"
            + "This could be due to incorrect jodi.properties where the jodi.properties are not in line with ODI,\n"
            + "e.g. check that the MODEL_CODE in ODI corresponds to the responding jodi.properties model.code,\n"
            + "or this could be due to an invalid XML file.";
    private final static String ERROR_MESSAGE_01020 = "The configuration property file is required to run Alter SCD Tables";
    private final TableServiceProvider tableService;
    private final TableService tableServiceCore;
    private final ErrorWarningMessageJodi errorWarningMessages;

    /**
     * Creates a new DeleteTransformationsActionRunner instance.
     *
     * @param tableService
     */
    @Inject
    protected AlterSCDTablesActionRunner(
            final TableServiceProvider tableService,
            final TableService tableServiceCore,
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.tableService = tableService;
        this.tableServiceCore = tableServiceCore;
        this.errorWarningMessages = errorWarningMessages;
    }

    /**
     * @see one.jodi.bootstrap.RunConfig$ActionRunner#run(RunConfig)
     */
    @Override
    public void run(final RunConfig config) {
        try {
            List<TableDefaultBehaviors> tableDefaults = tableServiceCore
                    .assembleDefaultBehaviors();
            tableService.alterSCDTables(tableDefaults);
        } catch (Exception ex) {
            throw new UnRecoverableException(
                    errorWarningMessages.formatMessage(1010,
                            ERROR_MESSAGE_01010, this.getClass()), ex);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * one.jodi.bootstrap.RunConfig.ActionRunner#validateRunConfig(oracle
     * .gbu.jodi.bootstrap.RunConfig)
     */
    @Override
    public void validateRunConfig(RunConfig config) throws UsageException {
        if (!StringUtils.hasLength(config.getPropertyFile())) {
            String msg = errorWarningMessages.formatMessage(1020, ERROR_MESSAGE_01020, this.getClass());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new UsageException(msg);
        }
    }

}
