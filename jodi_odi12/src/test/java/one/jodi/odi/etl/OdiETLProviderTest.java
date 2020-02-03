package one.jodi.odi.etl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.odi.common.FlexfieldUtil;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.model.finder.IOdiDataStoreFinder;
import oracle.odi.domain.model.finder.IOdiModelFinder;
import oracle.odi.domain.project.*;
import oracle.odi.domain.project.finder.IOdiCKMFinder;
import oracle.odi.domain.project.finder.IOdiIKMFinder;
import oracle.odi.domain.project.finder.IOdiLKMFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The class <code>OdiETLProviderTest</code> contains tests for the class <code>{@link Odi11ETLProvider}</code>.
 */
public class OdiETLProviderTest {
    @Mock
    OdiInstance odiInstance;
    @Mock
    JodiProperties properties;
    @Mock
    DatabaseMetadataService databaseMetadataService;
    @Mock
    ModelPropertiesProvider modelPropProvider;
    @Mock
    FlexfieldUtil<OdiModel> modelFlexfieldUtil;
    @Mock
    FlexfieldUtil<OdiDataStore> dataStoreFlexfieldUtil;
    @Mock
    OdiVariableAccessStrategy odiVariableService;
    @Mock
    IOdiDataStoreFinder dataStoreFinder;
    @Mock
    IOdiModelFinder modelFinder;
    @Mock
    IOdiEntityManager transactionalEntityManager;
    @Mock
    IOdiIKMFinder ikmFinder;
    @Mock
    IOdiCKMFinder ckmFinder;
    @Mock
    IOdiLKMFinder lkmFinder;
    String prefix = "test";
    String projectCode = "PCODE";
    OdiETLProvider fixture;

    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    /**
     * Perform pre-test initialization.
     *
     * @throws Exception if the initialization fails for some reason
     */
    @Before
    public void setUp()
            throws Exception {
        MockitoAnnotations.initMocks(this);
        when(properties.getProperty(JodiConstants.DATA_MART_PREFIX)).thenReturn(JodiConstants.DATA_MART_PREFIX);
        when(transactionalEntityManager.getFinder(OdiDataStore.class)).thenReturn(dataStoreFinder);
        when(transactionalEntityManager.getFinder(OdiModel.class)).thenReturn(modelFinder);
        when(transactionalEntityManager.getFinder(OdiIKM.class)).thenReturn(ikmFinder);
        when(transactionalEntityManager.getFinder(OdiCKM.class)).thenReturn(ckmFinder);
        when(transactionalEntityManager.getFinder(OdiLKM.class)).thenReturn(lkmFinder);
        when(properties.getProjectCode()).thenReturn(projectCode);
        when(odiInstance.getTransactionalEntityManager()).thenReturn(transactionalEntityManager);
        fixture = new OdiETLProvider(odiInstance, modelFlexfieldUtil,
                dataStoreFlexfieldUtil, odiVariableService,
                properties, modelPropProvider, errorWarningMessages);

    }


    /**
     * Perform post-test clean-up.
     *
     * @throws Exception if the clean-up fails for some reason
     */
    @After
    public void tearDown()
            throws Exception {
    }

    @Test
    public void testFindLKMByName() throws Exception {
        String name = "lkm";
        OdiLKM lkm = new OdiLKM(mock(OdiProject.class), projectCode);

        when(lkmFinder.findByName(name, projectCode)).thenReturn(Collections.<OdiLKM>singletonList(lkm));
        OdiKM<?> result = fixture.findLKMByName(name, projectCode);
        assertNotNull(result);
        assertSame(lkm, result);
    }

    @Test
    public void testFindCKMByName() throws Exception {
        String name = "ckm";
        OdiCKM ckm = new OdiCKM(mock(OdiProject.class), projectCode);

        when(ckmFinder.findByName(name, projectCode)).thenReturn(Collections.<OdiCKM>singletonList(ckm));
        OdiKM<?> result = fixture.findCKMByName(name, projectCode);
        assertNotNull(result);
        assertSame(ckm, result);
    }

    @Test
    public void testFindIKMByName() throws Exception {
        String name = "ikm";
        OdiIKM ikm = new OdiIKM(mock(OdiProject.class), projectCode);//mock(OdiKM.class);

        when(ikmFinder.findByName(name, projectCode)).thenReturn(Collections.<OdiIKM>singletonList(ikm));
        OdiKM<?> result = fixture.findIKMByName(name, projectCode);
        assertNotNull(result);
        assertSame(ikm, result);
    }
}
