package one.jodi.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import one.jodi.CreateEtlsImpl;
import one.jodi.action.AlterSCDTablesActionRunner;
import one.jodi.action.AlterTablesActionRunner;
import one.jodi.action.CheckTablesActionRunner;
import one.jodi.action.CreateConstraintsActionRunner;
import one.jodi.action.CreatePackagesActionRunner;
import one.jodi.action.CreateProcedureActionRunner;
import one.jodi.action.CreateScenariosActionRunner;
import one.jodi.action.CreateSequenceActionRunner;
import one.jodi.action.CreateTransformationsActionRunner;
import one.jodi.action.CreateVariableActionRunner;
import one.jodi.action.DeleteAllPackagesActionRunner;
import one.jodi.action.DeleteConstraintsActionRunner;
import one.jodi.action.DeletePackageActionRunner;
import one.jodi.action.DeleteProcedureActionRunner;
import one.jodi.action.DeleteReferencesActionRunner;
import one.jodi.action.DeleteScenarioActionRunner;
import one.jodi.action.DeleteSequenceActionRunner;
import one.jodi.action.DeleteTransformationsActionRunner;
import one.jodi.action.DeleteVariableActionRunner;
import one.jodi.action.EtlDataStoreBuildActionRunner;
import one.jodi.action.ExportConstraintsActionRunner;
import one.jodi.action.ExportSequencesActionRunner;
import one.jodi.action.ExportServiceActionRunner;
import one.jodi.action.ExportVariableActionrunner;
import one.jodi.action.ExtractTablesActionRunner;
import one.jodi.action.ImportServiceActionRunner;
import one.jodi.action.LoadPlanActionRunner;
import one.jodi.action.LoadPlanExportActionRunner;
import one.jodi.action.LoadPlanPrintActionRunner;
import one.jodi.action.PrintTransformationServiceActionRunner;
import one.jodi.action.ValidationActionRunner;
import one.jodi.base.bootstrap.ActionRunner;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.bootstrap.EtlRunConfig.ActionType;

/**
 * A <code>Guice</code> {@link com.google.inject.Module} implementation that
 * configures services necessary for application bootstrap.
 */
public class BootstrapModule extends AbstractModule {
    private static final String EOL = System.getProperty("line.separator");

    /**
     * Creates a new BootstrapModule instance.
     *
     * @param config {@link RunConfig}
     */
    public BootstrapModule(final RunConfig config) {
        super();
        if (EOL.length() > 10) {
            throw new UnRecoverableException(
                    "Possible DoS Attack via method call " + "System.getProperty(\"line.separator\")");
        }
    }

    /**
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    protected void configure() {

        // Register named ActionRunner types
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.CREATE_ETLS.getCode()))
                                .to(CreateEtlsImpl.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.CREATE_TRANSFORMATIONS.getCode()))
                                .to(CreateTransformationsActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.DELETE_TRANSFORMATIONS.getCode()))
                                .to(DeleteTransformationsActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.DELETE_PACKAGE.getCode()))
                                .to(DeletePackageActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.DELETE_SCENARIO.getCode()))
                                .to(DeleteScenarioActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.ALTER_SCD_TABLES.getCode()))
                                .to(AlterSCDTablesActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.ALTER_TABLES.getCode()))
                                .to(AlterTablesActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.CHECK_TABLES.getCode()))
                                .to(CheckTablesActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.EXTRACTION_TABLES.getCode()))
                                .to(ExtractTablesActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.CREATE_PACKAGES.getCode()))
                                .to(CreatePackagesActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.DELETE_REFERENCES.getCode()))
                                .to(DeleteReferencesActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.DELETE_ALL_PACKAGES.getCode()))
                                .to(DeleteAllPackagesActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.ODI_IMPORT.getCode()))
                                .to(ImportServiceActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.ODI_EXPORT.getCode()))
                                .to(ExportServiceActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.LOAD_PLAN_EXPORT.getCode()))
                                .to(LoadPlanExportActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.LOAD_PLAN_PRINT.getCode()))
                                .to(LoadPlanPrintActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.LOAD_PLAN.getCode()))
                                .to(LoadPlanActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.VARIABLES_EXPORT.getCode()))
                                .to(ExportVariableActionrunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.VARIABLES_CREATE.getCode()))
                                .to(CreateVariableActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.VARIABLES_DELETE.getCode()))
                                .to(DeleteVariableActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.SEQUENCES_EXPORT.getCode()))
                                .to(ExportSequencesActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.SEQUENCES_CREATE.getCode()))
                                .to(CreateSequenceActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.SEQUENCES_DELETE.getCode()))
                                .to(DeleteSequenceActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.CONSTRAINTS_CREATE.getCode()))
                                .to(CreateConstraintsActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.CONSTRAINTS_DELETE.getCode()))
                                .to(DeleteConstraintsActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.CONSTRAINTS_EXPORT.getCode()))
                                .to(ExportConstraintsActionRunner.class);

        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.PROCEDURE_CREATE.getCode()))
                                .to(CreateProcedureActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.PROCEDURE_DELETE.getCode()))
                                .to(DeleteProcedureActionRunner.class);

        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.PRINT.getCode()))
                                .to(PrintTransformationServiceActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.CREATE_SCENARIOS.getCode()))
                                .to(CreateScenariosActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.VALIDATE.getCode()))
                                .to(ValidationActionRunner.class);
        bind(ActionRunner.class).annotatedWith(Names.named(ActionType.CREATE_DATASTORES.getCode()))
                                .to(EtlDataStoreBuildActionRunner.class);
    }

}
