package one.jodi.core.service.impl;

import junit.framework.TestCase;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.builder.EnrichingBuilder;
import one.jodi.etl.builder.impl.EnrichingBuilderImpl;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.service.scenarios.ScenarioServiceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;


/**
 * The class <code>ScenarioServiceImplTest</code> contains tests for the class
 * {@link ScenarioServiceImpl}
 */
@RunWith(JUnit4.class)
public class ScenarioServiceImplTest extends TestCase {
    ScenarioServiceProvider mockProvider;
    JodiProperties properties;
    EnrichingBuilder enrichingBuilder;
    ScenarioServiceImpl fixture;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    /**
     * Creates a new ScenarioServiceImplTest instance.
     */
    public ScenarioServiceImplTest() {
    }

    @Override
    @Before
    public void setUp() throws Exception {
        mockProvider = mock(ScenarioServiceProvider.class);
        properties = mock(JodiProperties.class);
        enrichingBuilder = mock(EnrichingBuilderImpl.class);
        fixture = new ScenarioServiceImpl(mockProvider, properties,
                errorWarningMessages, enrichingBuilder);
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    @After
    public void tearDown() throws Exception {
    }

    /**
     * DOCUMENT ME!
     */
    @Test
    public void testDeleteScenarioListOfString() {
        String projectCode = "PCODE";
        List<ETLPackageHeader> headers = new ArrayList<>();

        headers.add(mock(ETLPackageHeader.class));
        headers.add(mock(ETLPackageHeader.class));
        headers.add(mock(ETLPackageHeader.class));

        when(properties.getProjectCode()).thenReturn(projectCode);

        fixture.deleteScenario(headers);
        verify(mockProvider).deleteScenarios(headers);
    }

    /**
     * DOCUMENT ME!
     */
    @Test
    public void testDeleteScenarioString() {
        String projectCode = "PCODE";
        String scenarioName = "testscenario";

        when(properties.getProjectCode()).thenReturn(projectCode);

        fixture.deleteScenario(scenarioName);
        verify(mockProvider).deleteScenario(scenarioName);

    }

    /**
     * DOCUMENT ME!
     */
    @Test
    public void testGenerateAllScenarios() {
        String projectCode = "PCODE";

        when(properties.getProjectCode()).thenReturn(projectCode);

        fixture.generateAllScenarios(new ArrayList<ETLPackageHeader>(), new ArrayList());
        verify(mockProvider).generateAllScenarios(new ArrayList<ETLPackageHeader>(), new ArrayList());
    }

}
