package one.jodi.odi12.packages;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.service.procedure.ProcedureServiceProvider;
import one.jodi.odi.common.OdiVersion;
import one.jodi.odi.packages.OdiPackageAccessStrategy;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.project.*;
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import oracle.odi.domain.topology.OdiLogicalSchema;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

;

@SuppressWarnings("deprecation")
public class OdiPackageServiceProviderTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none()
            .handleAssertionErrors();
    @Mock
    OdiInstance odiInstance;
    @Mock
    JournalizingContext journalizingContext;
    @Mock
    IOdiProjectFinder projectFinder;
    @Mock
    IOdiEntityManager entityManager;
    @Mock
    OdiProject odiProject;
    @Mock
    JodiProperties jodiProperties;
    @Mock
    ProcedureServiceProvider procedureService;
    @Mock
    OdiPackageAccessStrategy<Mapping, StepMapping> packageAccessStrategy;
    @Mock
    OdiVariableAccessStrategy odiVariableService;
    @Mock
    DatabaseMetadataService databaseMetadataService;
    String projectCode = "PCODE";
    Odi12PackageServiceProviderImpl fixture;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    public static void main(String[] args) {
        new org.junit.runner.JUnitCore()
                .run(OdiPackageServiceProviderTest.class);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(jodiProperties.getProjectCode()).thenReturn(projectCode);
        when(odiInstance.getTransactionalEntityManager()).thenReturn(entityManager);
        when(projectFinder.findByCode(projectCode)).thenReturn(odiProject);

        fixture = new Odi12PackageServiceProviderImpl(odiInstance, packageAccessStrategy,
                procedureService, odiVariableService,
                jodiProperties,
                journalizingContext,
                errorWarningMessages,
                new OdiVersion(),
                databaseMetadataService);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCreatePackage() {
        ETLPackage mockPackage = mock(ETLPackage.class);
        when(mockPackage.getFolderCode()).thenReturn("folder");
        when(mockPackage.getPackageListItems()).thenReturn("");
        OdiPackage odiPackage = mock(OdiPackage.class);
        Mapping odiInterface1 = mock(Mapping.class);
        Mapping odiInterface2 = mock(Mapping.class);
        Mapping odiInterface3 = mock(Mapping.class);
        when(packageAccessStrategy.findPackage(anyString(), anyString(), anyString()))
                .thenReturn(odiPackage);
        when(mockPackage.getPackageName()).thenReturn("PACKAGE");
        OdiFolder odiFolder = mock(OdiFolder.class);

        when(odiFolder.getFolderId()).thenReturn(1);
        when(odiFolder.getName()).thenReturn("folder");
        when(odiPackage.getParentFolder()).thenReturn(odiFolder);
        when(odiInterface1.getName()).thenReturn("Interface1");
        when(odiInterface2.getName()).thenReturn("Interface2");
        when(odiInterface3.getName()).thenReturn("Interface3");
        when(odiFolder.getMappings())
                .thenReturn(Arrays.asList(odiInterface1, odiInterface2, odiInterface3));

        // thrown.expect(DomainRuntimeException.class);
        // thrown.expectMessage("ODI-17654: Parameter Interface must be not null.");
        doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            OdiPackage pack = (OdiPackage) args[0];
            assertEquals("PACKAGE", pack.getName());
            // TODO: fill this in with more validations
            return null;

        }).when(entityManager).persist(any(OdiProject.class));

        fixture.createPackage(mockPackage, projectCode, true);
    }

    @Test
    public void testProjectVariableExists_defined() {
        String variable = "MyVariable";
        OdiVariable odiVariable = mock(OdiVariable.class);
        OdiLogicalSchema logicalSchema = mock(OdiLogicalSchema.class);
        when(odiVariable.getLogicalSchema()).thenReturn(logicalSchema);
        when(databaseMetadataService.projectVariableExists(projectCode, variable)).thenReturn(true);
        boolean exists = fixture.projectVariableExists(projectCode, variable);
        assertEquals(true, exists);
    }

    @Test
    public void testProjectVariableExists_undefined() {
        String variable = "MyVariable";

        boolean exists = fixture.projectVariableExists(projectCode, variable);
        assertEquals(false, exists);
    }
}
