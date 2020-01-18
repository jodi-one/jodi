package one.jodi.core.service.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.service.files.FileCollector;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.core.procedure.*;
import one.jodi.core.service.ProcedureException;
import one.jodi.etl.builder.ProcedureTransformationBuilder;
import one.jodi.etl.builder.impl.DictionaryModelLogicalSchema;
import one.jodi.etl.builder.impl.ProcedureTransformationBuilderImpl;
import one.jodi.etl.internalmodel.procedure.CommandInternal;
import one.jodi.etl.internalmodel.procedure.ProcedureHeader;
import one.jodi.etl.internalmodel.procedure.ProcedureInternal;
import one.jodi.etl.internalmodel.procedure.TaskInternal;
import one.jodi.etl.service.procedure.ProcedureServiceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProcedureServiceImplTest {

    final static Logger logger =
            LogManager.getLogger(ProcedureServiceImplTest.class);
    private final static String myLogicalModel = "myLogicalModel";
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();
    private List<Path> paths;
    private ObjectFactory factory;
    private ModelPropertiesProvider modelPropProv;
    private DictionaryModelLogicalSchema dictionaryModelLogicalSchema;
    // target interfaces for ODi 12c API
    private SchemaMetaDataProvider metadataProvider;
    private ProcedureServiceProvider procServiceProvider;
    // class under test
    private ProcedureServiceImpl fixture;
    private TestCollectXmlObjectsUtil<Procedure, ObjectFactory> xmlObjectsUtil;

    @After
    public void cleanup() {
        errorWarningMessages.printMessages(MESSAGE_TYPE.ERRORS);
        errorWarningMessages.printMessages(MESSAGE_TYPE.WARNINGS);
        errorWarningMessages.clear();
    }

    @Before
    public void setUp() throws Exception {
        errorWarningMessages.clear();
        factory = new ObjectFactory();

        dictionaryModelLogicalSchema = mock(DictionaryModelLogicalSchema.class);
        when(dictionaryModelLogicalSchema.translateToLogicalSchema("model.id.code")).thenReturn(myLogicalModel);
        final FileCollector fileCollector = mock(FileCollector.class);

        paths = new ArrayList<>();
        when(fileCollector.collectInPath(any(Path.class), anyString(), anyString(),
                anyString())).thenReturn(paths);

        final JodiProperties properties = mock(JodiProperties.class);
        when(properties.getProjectCode()).thenReturn("MY_PROJECT");

        // metadata service interface called for logical schema information
        // used in validation of model
        metadataProvider = mock(SchemaMetaDataProvider.class);
        Set<String> logicalModels = new HashSet<>();
        logicalModels.add(myLogicalModel);
        when(metadataProvider.getLogicalSchemaNames()).thenReturn(logicalModels);

        // target interface with ODI 12c
        // ensure that internal model is passed correctly to interface
        procServiceProvider = mock(ProcedureServiceProvider.class);

        modelPropProv = mock(ModelPropertiesProvider.class);
        List<ModelProperties> list = new ArrayList<ModelProperties>();
        ModelProperties mp = new ModelProperties() {

            @Override
            public boolean isJournalized() {
                return false;
            }

            @Override
            public void setJournalized(boolean journalized) {

            }

            @Override
            public boolean isIgnoredByHeuristics() {
                return false;
            }

            @Override
            public boolean isDefault() {
                return false;
            }

            @Override
            public List<String> getSubscribers() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public List<String> getPrefix() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public List<String> getPostfix() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int getOrder() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String getModelID() {
                // TODO Auto-generated method stub
                return "model.id";
            }

            @Override
            public String getLayer() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public List<String> getJkmoptions() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getJkm() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getCode() {
                return myLogicalModel;
            }

            @Override
            public int compareOrderTo(ModelProperties otherModelProperties) {
                // TODO Auto-generated method stub
                return 0;
            }
        };
        list.add(mp);
        when(modelPropProv.getConfiguredModels()).thenReturn(list);

        // TODO change visibility of constructor back to package
        final ProcedureTransformationBuilder procTransformationBuilder =
                new ProcedureTransformationBuilderImpl(metadataProvider,
                        errorWarningMessages,
                        this.dictionaryModelLogicalSchema);

        fixture = new ProcedureServiceImpl(fileCollector, properties,
                procTransformationBuilder,
                procServiceProvider, errorWarningMessages);
        this.xmlObjectsUtil = new TestCollectXmlObjectsUtil<>(ObjectFactory.class,
                JodiConstants.XSD_FILE_PROCEDURE,
                errorWarningMessages);
        Map<String, String> dictionary = new HashMap<String, String>();
        dictionary.put(myLogicalModel, myLogicalModel);
        when(metadataProvider.translateModelToLogicalSchema()).thenReturn(dictionary);
        fixture.setCollectXmlObjectUtil(xmlObjectsUtil);

    }

    private SourceCommandType createSCommand(String expression, String logicalSchema) {
        SourceCommandType command = factory.createSourceCommandType();
        command.setCommand(expression);
        command.setLogicalSchema(logicalSchema);
        return command;
    }

    private TargetCommandType createTCommand(String expression, String logicalSchema) {
        TargetCommandType command = factory.createTargetCommandType();
        command.setCommand(expression);
        command.setLogicalSchema(logicalSchema);
        return command;
    }

    private Task createTask(int line, String expression, String logicalSchema) {
        Task task = factory.createTask();
        task.setName("Line " + line);
        switch (line % 5) {
            case 0:
                task.setSourceCommand(createSCommand(expression + " S" + line,
                        logicalSchema));
                break;
            case 1:
                task.setTargetCommand(createTCommand(expression + " T" + line,
                        logicalSchema));
                break;
            case 2:
                task.setSourceCommand(createSCommand(expression + " S" + line,
                        logicalSchema));
                task.setTargetCommand(createTCommand(expression + " T" + line,
                        logicalSchema));
                break;
            case 3:
                task.setTargetCommand(createTCommand(null, logicalSchema));
                break;
            default:
                // no tasks set
        }
        return task;
    }

    private Procedure createProcedure(String folderName, String name,
                                      List<String> expressions, String logicalSchema) {
        Procedure procedure = factory.createProcedure();
        procedure.setFolderName(folderName);
        procedure.setName(name);
        procedure.setDescription("a description");

        List<Task> taskList = new ArrayList<>();
        for (int i = 0; i < expressions.size(); i++) {
            taskList.add(createTask(i, expressions.get(i), logicalSchema));
        }

        Tasks tasks = factory.createTasks();
        tasks.getTask().addAll(taskList);
        procedure.setTasks(tasks);

        return procedure;
    }

    private Map<Path, Procedure> setupFixture(final int countProcedures,
                                              final String folderPath,
                                              final int linesCount,
                                              final String procedureNameId,
                                              final String logicalSchemaName) {
        Map<Path, Procedure> procedures = new LinkedHashMap<>();
        IntStream.range(0, countProcedures).forEach(
                i -> {
                    List<String> expressions = new ArrayList<>();
                    String name = procedureNameId != null ? procedureNameId + i : "";
                    IntStream.range(0, linesCount)
                            .forEach(j -> expressions.add("this expression " + j));
                    Procedure procedure = createProcedure(folderPath, name, expressions,
                            logicalSchemaName);
                    Path path = Paths.get("./XML/" + folderPath + "/Procedure-" +
                            name + ".xml");
                    this.paths.add(path);
                    procedures.put(path, procedure);
                }
        );
        return procedures;
    }

    @Test
    public void testCreateMissingTask() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureInternal>> procedureArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, "root", 0, "Test 1 ", "model.id.code"));
        fixture.create("localpath", true);
        assertEquals(1, errorWarningMessages.getErrorMessages().get(-1).size());

        verify(this.metadataProvider, times(1)).getLogicalSchemaNames();
        verify(this.procServiceProvider, never()).createProcedures(procedureArg.capture(),
                eq(true),
                eq("MY_PROJECT"));
    }

    @Test
    public void testCreateOneTask() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureInternal>> procedureArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, "root", 1, "Test 2 ", "model.id.code"));
        fixture.create("localpath", true);
        assertTrue(errorWarningMessages.getErrorMessages().isEmpty());

        verify(this.metadataProvider, times(1)).getLogicalSchemaNames();
        verify(this.procServiceProvider, times(1)).createProcedures(procedureArg.capture(),
                eq(true),
                eq("MY_PROJECT"));
        List<ProcedureInternal> procedures = procedureArg.getValue();
        assertNotNull(procedures);
        assertEquals(1, procedures.size());

        ProcedureInternal procedure = procedures.get(0);
        assertEquals("Test 2 0", procedure.getName());
        assertEquals("." + File.separator + "XML" + File.separator + "root" +
                File.separator + "Procedure-Test 2 0.xml", procedure.getFilePath());
        assertEquals("ORACLE", procedure.getSourceTechnology());
        assertEquals("ORACLE", procedure.getTargetTechnology());
        assertEquals(1, procedure.getFolderNames().size());
        assertEquals("root", procedure.getFolderNames().get(0));
        assertTrue(procedure.getDescription().isPresent());
        assertEquals("a description", procedure.getDescription().get());
        assertEquals(1, procedure.getTasks().size());
        assertFalse(procedure.isMultiConnectionSupported());
        assertFalse(procedure.isRemoveTemporaryObjectsonError());
        assertFalse(procedure.isUseUniqueTemporaryObjectNames());

        TaskInternal task = procedure.getTasks().get(0);
        assertEquals("Line 0", task.getName());
        assertEquals(procedure, task.getParent());

        assertTrue(task.getSourceCommand().isPresent());
        assertFalse(task.getTargetCommand().isPresent());

        // default values
        assertEquals(5, task.getLogLevel());
        assertEquals("NONE", task.getLogCounter());
        assertFalse(task.isCleanup());
        assertFalse(task.isIgnoreErrors());
        assertFalse(task.isLogFinalCommand());

        CommandInternal srcCommand = task.getSourceCommand().get();
        assertEquals(task, srcCommand.getParent());
        assertEquals("this expression 0 S0", srcCommand.getCommand());
        assertEquals(myLogicalModel, srcCommand.getLogicalSchema());

        // default values
        assertEquals("ORACLE", srcCommand.getTechnology());
        assertEquals("NONE", srcCommand.getTransactionIsolation());
        assertEquals("EXECUTION_CONTEXT", srcCommand.getExecutionContext());
        assertEquals("Autocommit", srcCommand.getTransaction());
        assertTrue(srcCommand.isAlwaysExecuteOptions());

    }

    @Test // three correctly formed lines
    public void testCreateThreeTasks() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureInternal>> procedureArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, "root", 3, "Test 3 ", "model.id.code"));
        fixture.create("localpath", false);
        errorWarningMessages.getErrorMessages().values().stream().peek(m -> System.out.println(m));
        assertTrue(errorWarningMessages.getErrorMessages().isEmpty());

        verify(this.metadataProvider, times(1)).getLogicalSchemaNames();
        verify(this.procServiceProvider, times(1)).createProcedures(procedureArg.capture(),
                eq(false),
                eq("MY_PROJECT"));
        List<ProcedureInternal> procedures = procedureArg.getValue();
        assertNotNull(procedures);

        ProcedureInternal procedure = procedures.get(0);
        assertEquals("Test 3 0", procedure.getName());

        assertEquals(3, procedure.getTasks().size());

        TaskInternal task1 = procedure.getTasks().get(0);
        assertEquals("Line 0", task1.getName());
        assertTrue(task1.getSourceCommand().isPresent());
        assertFalse(task1.getTargetCommand().isPresent());

        CommandInternal srcCommand = task1.getSourceCommand().get();
        assertEquals(task1, srcCommand.getParent());
        assertEquals("this expression 0 S0", srcCommand.getCommand());
        assertEquals(myLogicalModel, srcCommand.getLogicalSchema());

        TaskInternal task2 = procedure.getTasks().get(1);
        assertEquals("Line 1", task2.getName());
        assertFalse(task2.getSourceCommand().isPresent());
        assertTrue(task2.getTargetCommand().isPresent());

        CommandInternal trgCommand = task2.getTargetCommand().get();
        assertEquals(task2, trgCommand.getParent());
        assertEquals("this expression 1 T1", trgCommand.getCommand());
        assertEquals(myLogicalModel, trgCommand.getLogicalSchema());

        TaskInternal task3 = procedure.getTasks().get(2);
        assertEquals("Line 2", task3.getName());
        assertTrue(task3.getSourceCommand().isPresent());
        assertTrue(task3.getTargetCommand().isPresent());
    }

    @Test // contains two defects in line 4 and 5
    public void testCreateFiveTasks() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureInternal>> procedureArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, "root", 5, "Test 5 ", "model.id.code"));


        fixture.create("localpath", true);
        assertEquals(2, errorWarningMessages.getErrorMessages().get(-1).size());

        verify(this.metadataProvider, times(1)).getLogicalSchemaNames();
        verify(this.procServiceProvider, never()).createProcedures(procedureArg.capture(),
                eq(true),
                eq("MY_PROJECT"));
    }

    @Test
    public void testCreateMultipleMissingElements() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureInternal>> procedureArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, "", 0, null, "model.id.code"));
        fixture.create("localpath", true);
        assertEquals(3, errorWarningMessages.getErrorMessages().get(-1).size());

        verify(this.metadataProvider, times(1)).getLogicalSchemaNames();
        verify(this.procServiceProvider, never()).createProcedures(procedureArg.capture(),
                eq(true),
                eq("MY_PROJECT"));
    }

    @Test
    public void testCreateMissingLogicalModelTask() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureInternal>> procedureArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, "root", 1, "Test 7 ", myLogicalModel));
        fixture.create("localpath", true);
        assertEquals(1, errorWarningMessages.getErrorMessages().get(-1).size());

        verify(this.metadataProvider, times(1)).getLogicalSchemaNames();
        verify(this.procServiceProvider, never()).createProcedures(procedureArg.capture(),
                eq(true),
                eq("MY_PROJECT"));
    }

    @Test
    public void testCreateUnknownLogicalModelTask() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureInternal>> procedureArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, null, 1, "Test 8 ", "unknownLogicalModel"));
        fixture.create("localpath", true);
        assertEquals(2, errorWarningMessages.getErrorMessages().get(-1).size());

        verify(this.metadataProvider, times(1)).getLogicalSchemaNames();
        verify(this.procServiceProvider, never()).createProcedures(procedureArg.capture(),
                eq(true),
                eq("MY_PROJECT"));
    }

    @Test
    public void testCreateIncorrectPath() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureInternal>> procedureArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, "root//dir", 1, "Test 9 ", "model.id.code"));
        fixture.create("localpath", true);
        assertEquals(1, errorWarningMessages.getErrorMessages().get(-1).size());

        verify(this.metadataProvider, times(1)).getLogicalSchemaNames();
        verify(this.procServiceProvider, never()).createProcedures(procedureArg.capture(),
                eq(true),
                eq("MY_PROJECT"));
    }

    @Test
    public void testDeleteOneTask() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureHeader>> procedureHeaderArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, "root", 1, "Test 10 ", "model.id.code"));
        fixture.delete("localpath");
        assertTrue(errorWarningMessages.getErrorMessages().isEmpty());

        verify(this.metadataProvider, never()).getLogicalSchemaNames();
        verify(this.procServiceProvider, times(1))
                .deleteProcedures(procedureHeaderArg.capture(),
                        eq("MY_PROJECT"));
        List<ProcedureHeader> procedureHeaders = procedureHeaderArg.getValue();
        assertNotNull(procedureHeaders);
    }

    @Test
    public void testDeleteMissingInfromation() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureHeader>> procedureHeaderArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, "root", 1, null, "model.id.code"));
        fixture.delete("localpath");
        assertEquals(1, errorWarningMessages.getErrorMessages().get(-1).size());

        verify(this.metadataProvider, never()).getLogicalSchemaNames();
        verify(this.procServiceProvider, never())
                .deleteProcedures(procedureHeaderArg.capture(),
                        eq("MY_PROJECT"));
    }

    @Test
    public void testCreateOneTaskWithException() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureInternal>> procedureArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, "root/dir", 1, "Test 20 ", "model.id.code"));
        // trigger exception
        ProcedureHeader intProc = mock(ProcedureHeader.class);
        when(intProc.getName()).thenReturn("Test 20 1");
        when(intProc.getFolderNames()).thenReturn(Arrays.asList("root", "dir"));
        ProcedureException pe = new ProcedureException("error occured",
                new RuntimeException("some ODI Issue"),
                Arrays.asList(intProc));
        doThrow(pe).when(this.procServiceProvider)
                .createProcedures(anyListOf(ProcedureInternal.class),
                        any(Boolean.class),
                        eq("MY_PROJECT"));
        fixture.create("localpath", true);
        assertEquals(1, errorWarningMessages.getErrorMessages().get(-1).size());

        verify(this.metadataProvider, times(1)).getLogicalSchemaNames();
        verify(this.procServiceProvider, times(1)).createProcedures(procedureArg.capture(),
                eq(true),
                eq("MY_PROJECT"));
        List<ProcedureInternal> procedures = procedureArg.getValue();
        assertNotNull(procedures);
    }

    @Test
    public void testDeleteOneTaskWithException() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<ProcedureHeader>> procedureHeaderArg =
                ArgumentCaptor.forClass((Class) List.class);
        this.xmlObjectsUtil.add(setupFixture(1, "root/dir", 1, "Test 30 ", "model.id.code"));
        // trigger exception
        ProcedureHeader intProc = mock(ProcedureHeader.class);
        when(intProc.getName()).thenReturn("Test 30 ");
        when(intProc.getFolderNames()).thenReturn(Arrays.asList("root", "dir"));
        ProcedureException pe = new ProcedureException("error occured",
                new RuntimeException("some ODI Issue"),
                Arrays.asList(intProc));
        doThrow(pe).when(this.procServiceProvider)
                .deleteProcedures(anyListOf(ProcedureHeader.class),
                        eq("MY_PROJECT"));

        fixture.delete("localpath");
        assertEquals(1, errorWarningMessages.getErrorMessages().get(-1).size());

        verify(this.metadataProvider, never()).getLogicalSchemaNames();
        verify(this.procServiceProvider, times(1))
                .deleteProcedures(procedureHeaderArg.capture(),
                        eq("MY_PROJECT"));
        List<ProcedureHeader> procedures = procedureHeaderArg.getValue();
        assertNotNull(procedures);
    }

}
