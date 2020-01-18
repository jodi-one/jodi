package one.jodi.core.validation.packages;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.context.packages.PackageCache;
import one.jodi.core.context.packages.TransformationCacheItem;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.internalmodel.VariableStep.VariableSetOperatorType;
import one.jodi.etl.internalmodel.VariableStep.VariableStepType;
import one.jodi.etl.service.packages.PackageServiceProvider;
import one.jodi.etl.service.packages.ProcedureNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageValidatorImplTest {
    @Mock
    PackageServiceProvider packageService;
    @Mock
    JournalizingContext journalizingContext;
    @Mock
    JodiProperties properties;
    @Mock
    PackageCache packageCache;
    @Mock
    DatabaseMetadataService databaseMetadataService;
    PackageValidatorImpl fixture;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();
    private String projectCode = "PCODE";

    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(PackageValidatorImplTest.class);
    }

    @Before
    public void setUp()
            throws Exception {
        MockitoAnnotations.initMocks(this);
        when(properties.getProjectCode()).thenReturn(projectCode);
        fixture = new PackageValidatorImpl(properties, journalizingContext,
                packageService, packageCache, errorWarningMessages, "/tmp", "false", databaseMetadataService);
    }

    @After
    public void tearDown()
            throws Exception {
    }

    @Test
    public void testValidatePackageExecProcedureNoErrors()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("FOLDER_NAME");
        List<ETLPackage> packages = Arrays.asList(p1);
        ProcedureStep procedureType = mock(ProcedureStep.class);
        when(p1.getFirstStep()).thenReturn(procedureType);
        when(packageService.getProcedureParameterNames(projectCode, "PROC1")).thenReturn(Arrays.asList("param1", "param2"));
        when(procedureType.getName()).thenReturn("PROC1");
        StepParameter param1 = mock(StepParameter.class);
        when(param1.getName()).thenReturn("param1");
        when(param1.getValue()).thenReturn("value1");
        StepParameter param2 = mock(StepParameter.class);
        when(param2.getName()).thenReturn("param2");
        when(param2.getValue()).thenReturn("value2");
        Collection<StepParameter> parameters = Arrays.asList(param1, param2);

        when(procedureType.getParameters()).thenReturn(parameters);
        TransformationCacheItem tci = new TransformationCacheItem("TRANSFORMATION_1", 1, "COMMON", "FOLDER_NAME", false);
        when(packageCache.getTransformationsForPackage("COMMON", "FOLDER_NAME")).thenReturn(Arrays.asList(tci));
        when(p1.getTargetPackageList()).thenReturn(Arrays.asList("COMMON"));

        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageListItems_NoAssocTrans() throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        ProcedureStep procedureType = mock(ProcedureStep.class);
        when(p1.getFirstStep()).thenReturn(procedureType);
        when(packageService.getProcedureParameterNames(projectCode, "PROC1")).thenReturn(Arrays.asList("param1", "param2"));
        when(procedureType.getName()).thenReturn("PROC1");
        StepParameter param1 = mock(StepParameter.class);
        when(param1.getName()).thenReturn("param1");
        when(param1.getValue()).thenReturn("value1");
        StepParameter param2 = mock(StepParameter.class);
        when(param2.getName()).thenReturn("param2");
        when(param2.getValue()).thenReturn("value2");
        Collection<StepParameter> parameters = Arrays.asList(param1, param2);

        when(procedureType.getParameters()).thenReturn(parameters);

        // ensure "" is skipped
        when(p1.getTargetPackageList()).thenReturn(Arrays.asList("COMMON", ""));
        when(packageCache.getTransformationsForPackage("COMMON", "FOLDER_NAME")).thenReturn(Collections.<TransformationCacheItem>emptyList());

        List<PackageValidationResult> results = fixture.validatePackages(packages);
        PackageValidationResult pvr = findResultForPackage(results, p1);
        assertValidationMessages(pvr, 80600);

    }

    //	@Test
    public void testValidatePackageExecPackageOutOfOrderReferenceNoErrors()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        ETLPackage p2 = mock(ETLPackage.class);
        when(p2.getPackageName()).thenReturn("P2");
        when(p2.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1, p2);
        PackageStep packageType = mock(PackageStep.class);
        ETLStep before = packageType;

        when(p1.getFirstStep()).thenReturn(before);
        when(packageService.packageExists("P2", "F1")).thenReturn(false);
        when(packageType.getName()).thenReturn("P2");
        when(packageType.executeAsynchronously()).thenReturn(false);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 2, result.size());
        assertValidationState(result, true, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr);
        assertPackageOrder(result, p1, p2);
    }

    @Test
    public void testValidatePackageExecProcedureInvalidPArameter()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        ProcedureStep procedureType = mock(ProcedureStep.class);
        ETLStep before = procedureType;

        when(p1.getFirstStep()).thenReturn(before);
        when(packageService.getProcedureParameterNames(projectCode, "PROC1")).thenReturn(Arrays.asList("param1", "param2"));
        when(procedureType.getName()).thenReturn("PROC1");
        StepParameter param1 = mock(StepParameter.class);
        when(param1.getName()).thenReturn("param1");
        when(param1.getValue()).thenReturn("value1");
        StepParameter param2 = mock(StepParameter.class);
        when(param2.getName()).thenReturn("param3");
        when(param2.getValue()).thenReturn("value3");
        when(procedureType.getParameters()).thenReturn(Arrays.asList(param1, param2));

        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, false);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80310);
        assertPackageOrder(result, p1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValidatePackageExecProcedureInvalidProcedureName()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        ProcedureStep procedureType = mock(ProcedureStep.class);
        ETLStep before = procedureType;

        when(p1.getFirstStep()).thenReturn(before);
        when(packageService.getProcedureParameterNames(projectCode, "PROC1")).thenThrow(ProcedureNotFoundException.class);
        when(procedureType.getName()).thenReturn("PROC1");
        StepParameter param1 = mock(StepParameter.class);
        when(param1.getName()).thenReturn("param1");
        when(param1.getValue()).thenReturn("value1");
        StepParameter param2 = mock(StepParameter.class);
        when(param2.getName()).thenReturn("param2");
        when(param2.getValue()).thenReturn("value2");
        when(procedureType.getParameters()).thenReturn(Arrays.asList(param1, param2));

        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, false);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80300);
        assertPackageOrder(result, p1);
    }

//	@Test
    public void testValidatePackageExecPackageNoErrors()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        PackageStep packageType = mock(PackageStep.class);

        when(p1.getFirstStep()).thenReturn(packageType);
        when(packageService.packageExists("PACK1", "F1")).thenReturn(true);
        when(packageType.getName()).thenReturn("PACK1");
        when(packageType.executeAsynchronously()).thenReturn(false);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr);
        assertPackageOrder(result, p1);
    }

    //	@Test
    public void testValidatePackageExecPackageNotExists()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        PackageStep packageType = mock(PackageStep.class);

        when(p1.getFirstStep()).thenReturn(packageType);
        when(packageService.packageExists("PACK1", "F1")).thenReturn(false);
        when(packageType.getName()).thenReturn("PACK1");
        when(packageType.executeAsynchronously()).thenReturn(false);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, false);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80200);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageDuplicatePackageDefinition()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        ETLPackage p2 = mock(ETLPackage.class);
        when(p2.getPackageName()).thenReturn("P1");
        when(p2.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1, p2);

        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        // TODO: remove showResults
        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 2, result.size());
        assertValidationState(result, true, false);

        PackageValidationResult pvr = findResultForPackage(result, p2);
        assertValidationMessages(pvr, 80000);
        assertPackageOrder(result, p1, p2);
    }

    @Test
    public void testValidatePackageNonExistentVariable()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(false);

        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, false);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80100);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageRefreshVariableNoErrors()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.REFRESH);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn(null);
        when(variable.getIncrementBy()).thenReturn(null);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageRefreshVariableInvalidOperator()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.REFRESH);
        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn("<");
        when(variable.getIncrementBy()).thenReturn(null);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80110);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageRefreshVariableInvalidValue()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.REFRESH);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getValue()).thenReturn("value");
        when(variable.getIncrementBy()).thenReturn(null);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80110);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageRefreshVariableInvalidSetOperator()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.REFRESH);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.INCREMENT);
        when(variable.getIncrementBy()).thenReturn(null);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80110);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageRefreshVariableInvalidIncrementBy()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.REFRESH);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getIncrementBy()).thenReturn(1);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80110);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageRefreshVariableMultipleInvalidFields()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.REFRESH);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn("=");
        when(variable.getIncrementBy()).thenReturn(1);
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.INCREMENT);
        when(variable.getValue()).thenReturn("value");
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80110);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageEvaluateVariableNoErrors()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.EVALUATE);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn("<");
        when(variable.getIncrementBy()).thenReturn(null);
        when(variable.getValue()).thenReturn("value");
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageSetVariableMissingValue()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.SET);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn(null);
        when(variable.getIncrementBy()).thenReturn(null);
        when(variable.getValue()).thenReturn(null);
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.ASSIGN);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, false);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80130);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageSetVariableMissingSetOperator()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.SET);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn(null);
        when(variable.getIncrementBy()).thenReturn(null);
        when(variable.getValue()).thenReturn("value");
        when(variable.getSetOperator()).thenReturn(null);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, false);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80130);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageSetVariableMissingSetOperatorAndValue()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.SET);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn(null);
        when(variable.getIncrementBy()).thenReturn(null);
        when(variable.getValue()).thenReturn(null);
        when(variable.getSetOperator()).thenReturn(null);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, false);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80130);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageSetVariableInvalidOperator()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.SET);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn("<");
        when(variable.getIncrementBy()).thenReturn(null);
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.INCREMENT);
        when(variable.getValue()).thenReturn("value");
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.ASSIGN);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80140);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageSetVariableInvalidIncrementBy()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.SET);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn("<");
        when(variable.getIncrementBy()).thenReturn(1);
        when(variable.getValue()).thenReturn("value");
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.ASSIGN);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80140);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageSetVariableMultipleInvalidFields()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.SET);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn("<");
        when(variable.getIncrementBy()).thenReturn(1);
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.INCREMENT);
        when(variable.getValue()).thenReturn("value");
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.ASSIGN);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80140);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageSetVariableAssignNoErrors()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.SET);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn(null);
        when(variable.getIncrementBy()).thenReturn(null);
        when(variable.getValue()).thenReturn("value");
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.ASSIGN);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageSetVariableIncrementNoErrors()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.SET);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn(null);
        when(variable.getIncrementBy()).thenReturn(2);
        when(variable.getValue()).thenReturn(null);
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.INCREMENT);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageEvaluateVariableMissingValue()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.EVALUATE);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn("<");
        when(variable.getIncrementBy()).thenReturn(null);
        when(variable.getValue()).thenReturn(null);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, false);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80120);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageEvaluateVariableMissingOperator()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.EVALUATE);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn(null);
        when(variable.getIncrementBy()).thenReturn(null);
        when(variable.getValue()).thenReturn("value");
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, false);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80120);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageEvaluateVariableMissingOperatorAndValue()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.EVALUATE);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn(null);
        when(variable.getIncrementBy()).thenReturn(null);
        when(variable.getValue()).thenReturn(null);
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, false);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80120);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageEvaluateVariableInvalidSetOperator()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.EVALUATE);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn("<");
        when(variable.getIncrementBy()).thenReturn(null);
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.INCREMENT);
        when(variable.getValue()).thenReturn("value");
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80121);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageEvaluateVariableInvalidIncrementBy()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.EVALUATE);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn("<");
        when(variable.getIncrementBy()).thenReturn(1);
        when(variable.getValue()).thenReturn("value");
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80121);
        assertPackageOrder(result, p1);
    }

    @Test
    public void testValidatePackageEvaluateVariableMultipleInvalidFields()
            throws Exception {
        ETLPackage p1 = mock(ETLPackage.class);
        when(p1.getPackageName()).thenReturn("P1");
        when(p1.getFolderCode()).thenReturn("F1");
        List<ETLPackage> packages = Arrays.asList(p1);
        VariableStep variable = mock(VariableStep.class);
        when(variable.getName()).thenReturn("V1");
        when(variable.getStepType()).thenReturn(VariableStepType.EVALUATE);

        when(p1.getFirstStep()).thenReturn(variable);
        when(databaseMetadataService.projectVariableExists(projectCode, "V1")).thenReturn(true);
        when(variable.getOperator()).thenReturn("<");
        when(variable.getIncrementBy()).thenReturn(1);
        when(variable.getSetOperator()).thenReturn(VariableSetOperatorType.INCREMENT);
        when(variable.getValue()).thenReturn("value");
        List<PackageValidationResult> result = fixture.validatePackages(packages);

        assertNotNull(result);

        showResults(result);
        assertEquals("Incorrect PackageValidationResult list size", 1, result.size());
        assertValidationState(result, true);

        PackageValidationResult pvr = findResultForPackage(result, p1);
        assertValidationMessages(pvr, 80121);
        assertPackageOrder(result, p1);
    }

    private void assertValidationState(List<PackageValidationResult> validationResults, boolean... expectedStates) {
        assertTrue("Expected result size of " + expectedStates.length + " but was " + validationResults.size(), expectedStates.length == validationResults.size());
        int idx = 0;

        while (idx < expectedStates.length && idx < validationResults.size()) {
            PackageValidationResult result = validationResults.get(idx);

            assertTrue("Validation result for package " + result.getTargetPackage().getPackageName() + " expected to be " + expectedStates[idx] + " but was " + result.isValid(), result.isValid() == expectedStates[idx]);
            idx++;
        }
    }

    private void assertValidationMessages(PackageValidationResult result, int... messageNumbers) {
        if (!result.hasMessages() && messageNumbers.length > 0) {
            fail("Package " + result.getTargetPackage().getPackageName() + " has no error messages but expected message size is not zero");
        }
        if (result.hasMessages() && messageNumbers.length == 0) {
            fail("Package " + result.getTargetPackage().getPackageName() + " has unexpected error messages");
        }

        List<String> messages = result.getValidationMessages();

        if (messages.size() > messageNumbers.length) {
            fail("Package " + result.getTargetPackage().getPackageName() + " has more error messages than expected");
        }
        for (int msgId : messageNumbers) {
            if (!hasMessage(messages, msgId)) {
                fail("Expected package " + result.getTargetPackage().getPackageName() + " to have error message " + msgId + " but it was not present");
            }
        }
    }

    private void assertPackageOrder(List<PackageValidationResult> validationResults, ETLPackage... packages) {
        assertTrue("Validation result size was " + validationResults.size() + " but expected " + packages.length, validationResults.size() == packages.length);

        for (int idx = 0; idx < packages.length; idx++) {
            ETLPackage found = validationResults.get(idx).getTargetPackage();
            assertTrue("Incorrect package at index " + idx + ". Expected package " + packages[idx].getPackageName() + " but found " + found.getPackageName(), found == packages[idx]);
        }
    }

    private boolean hasMessage(List<String> messages, int msgId) {
        for (String message : messages) {
            String temp = "[" + Integer.toString(msgId) + "]";
            if (message.startsWith(temp)) {
                return true;
            }
        }
        return false;
    }

    private PackageValidationResult findResultForPackage(List<PackageValidationResult> results, ETLPackage p) {
        for (PackageValidationResult result : results) {
            if (p == result.getTargetPackage()) {
                return result;
            }
        }

        return null;
    }

    // TODO: Remove showResults
    private void showResults(List<PackageValidationResult> results) {
        for (PackageValidationResult result : results) {
            for (String msg : result.getValidationMessages()) {
                System.out.println(msg);
            }
        }
    }

}
