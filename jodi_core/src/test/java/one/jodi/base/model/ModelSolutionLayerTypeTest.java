package one.jodi.base.model;

import one.jodi.base.model.types.ModelSolutionLayer;
import one.jodi.base.model.types.ModelSolutionLayerType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The class <code>ModelSolutionLayerTypeTest</code> contains tests for the class <code>{@link ModelSolutionLayerType}</code>.
 */
public class ModelSolutionLayerTypeTest {

    @Before
    public void setUp()
            throws Exception {
    }

    @After
    public void tearDown()
            throws Exception {
    }

    @Test
    public void testGetSolutionLayerName_EDW()
            throws Exception {
        ModelSolutionLayerType fixture = ModelSolutionLayerType.EDW;

        String result = fixture.getSolutionLayerName();
        assertEquals("edw", result);
    }

    @Test
    public void testGetSolutionLayerName_EDW_SDS()
            throws Exception {
        ModelSolutionLayerType fixture = ModelSolutionLayerType.EDW_SDS;

        String result = fixture.getSolutionLayerName();
        assertEquals("edw_sds", result);
    }

    @Test
    public void testGetSolutionLayerName_EDW_SIS()
            throws Exception {
        ModelSolutionLayerType fixture = ModelSolutionLayerType.EDW_SIS;

        String result = fixture.getSolutionLayerName();
        assertEquals("edw_sis", result);
    }

    @Test
    public void testGetSolutionLayerName_SOURCE()
            throws Exception {
        ModelSolutionLayerType fixture = ModelSolutionLayerType.SOURCE;

        String result = fixture.getSolutionLayerName();
        assertEquals("source", result);
    }

    @Test
    public void testGetSolutionLayerName_STAR()
            throws Exception {
        ModelSolutionLayerType fixture = ModelSolutionLayerType.STAR;

        String result = fixture.getSolutionLayerName();
        assertEquals("star", result);
    }

    @Test
    public void testGetSolutionLayerName_STAR_SDS()
            throws Exception {
        ModelSolutionLayerType fixture = ModelSolutionLayerType.STAR_SDS;

        String result = fixture.getSolutionLayerName();
        assertEquals("star_sds", result);
    }

    @Test
    public void testGetSolutionLayerName_STAR_SIS()
            throws Exception {
        ModelSolutionLayerType fixture = ModelSolutionLayerType.STAR_SIS;

        String result = fixture.getSolutionLayerName();
        assertEquals("star_sis", result);
    }

    @Test
    public void testGetSolutionLayerName_UNKNOWN()
            throws Exception {
        ModelSolutionLayerType fixture = ModelSolutionLayerType.UNKNOWN;

        String result = fixture.getSolutionLayerName();
        assertEquals("unknown", result);
    }

    @Test
    public void testModelSolutionLayerFor_NullLayerName()
            throws Exception {
        String layerName = null;

        ModelSolutionLayer result = ModelSolutionLayerType.modelSolutionLayerFor(layerName);

        assertNotNull(result);
        assertEquals("unknown", result.getSolutionLayerName());
        assertEquals(ModelSolutionLayerType.UNKNOWN, result);
    }

    @Test
    public void testModelSolutionLayerFor_LayerNameEDW()
            throws Exception {
        String layerName = "edw";

        ModelSolutionLayer result = ModelSolutionLayerType.modelSolutionLayerFor(layerName);

        assertNotNull(result);
        assertEquals(layerName, result.getSolutionLayerName());

        ModelSolutionLayer result2 = ModelSolutionLayerType.modelSolutionLayerFor(layerName.toUpperCase());
        assertNotNull(result2);
        assertEquals(layerName, result2.getSolutionLayerName());
        assertSame(result, result2);
        assertEquals(ModelSolutionLayerType.EDW, result);
    }

    @Test
    public void testModelSolutionLayerFor_LayerNameEDW_SIS()
            throws Exception {
        String layerName = "edw_sis";

        ModelSolutionLayer result = ModelSolutionLayerType.modelSolutionLayerFor(layerName);

        assertNotNull(result);
        assertEquals(layerName, result.getSolutionLayerName());

        ModelSolutionLayer result2 = ModelSolutionLayerType.modelSolutionLayerFor(layerName.toUpperCase());
        assertNotNull(result2);
        assertEquals(layerName, result2.getSolutionLayerName());
        assertSame(result, result2);
        assertEquals(ModelSolutionLayerType.EDW_SIS, result);
    }

    @Test
    public void testModelSolutionLayerFor_LayerNameEDW_SDS()
            throws Exception {
        String layerName = "edw_sds";

        ModelSolutionLayer result = ModelSolutionLayerType.modelSolutionLayerFor(layerName);

        assertNotNull(result);
        assertEquals(layerName, result.getSolutionLayerName());

        ModelSolutionLayer result2 = ModelSolutionLayerType.modelSolutionLayerFor(layerName.toUpperCase());
        assertNotNull(result2);
        assertEquals(layerName, result2.getSolutionLayerName());
        assertSame(result, result2);
        assertEquals(ModelSolutionLayerType.EDW_SDS, result);
    }

    @Test
    public void testModelSolutionLayerFor_LayerNameSOURCE()
            throws Exception {
        String layerName = "source";

        ModelSolutionLayer result = ModelSolutionLayerType.modelSolutionLayerFor(layerName);

        assertNotNull(result);
        assertEquals(layerName, result.getSolutionLayerName());

        ModelSolutionLayer result2 = ModelSolutionLayerType.modelSolutionLayerFor(layerName.toUpperCase());
        assertNotNull(result2);
        assertEquals(layerName, result2.getSolutionLayerName());
        assertSame(result, result2);
        assertEquals(ModelSolutionLayerType.SOURCE, result);
    }

    @Test
    public void testModelSolutionLayerFor_LayerNameSTAR()
            throws Exception {
        String layerName = "star";

        ModelSolutionLayer result = ModelSolutionLayerType.modelSolutionLayerFor(layerName);

        assertNotNull(result);
        assertEquals(layerName, result.getSolutionLayerName());

        ModelSolutionLayer result2 = ModelSolutionLayerType.modelSolutionLayerFor(layerName.toUpperCase());
        assertNotNull(result2);
        assertEquals(layerName, result2.getSolutionLayerName());
        assertSame(result, result2);
        assertEquals(ModelSolutionLayerType.STAR, result);
    }

    @Test
    public void testModelSolutionLayerFor_LayerNameSTAR_SDS()
            throws Exception {
        String layerName = "star_sds";

        ModelSolutionLayer result = ModelSolutionLayerType.modelSolutionLayerFor(layerName);

        assertNotNull(result);
        assertEquals(layerName, result.getSolutionLayerName());

        ModelSolutionLayer result2 = ModelSolutionLayerType.modelSolutionLayerFor(layerName.toUpperCase());
        assertNotNull(result2);
        assertEquals(layerName, result2.getSolutionLayerName());
        assertSame(result, result2);
        assertEquals(ModelSolutionLayerType.STAR_SDS, result);
    }

    @Test
    public void testModelSolutionLayerFor_LayerNameSTAR_SIS()
            throws Exception {
        String layerName = "star_sis";

        ModelSolutionLayer result = ModelSolutionLayerType.modelSolutionLayerFor(layerName);

        assertNotNull(result);
        assertEquals(layerName, result.getSolutionLayerName());

        ModelSolutionLayer result2 = ModelSolutionLayerType.modelSolutionLayerFor(layerName.toUpperCase());
        assertNotNull(result2);
        assertEquals(layerName, result2.getSolutionLayerName());
        assertSame(result, result2);
        assertEquals(ModelSolutionLayerType.STAR_SIS, result);
    }

    @Test
    public void testModelSolutionLayerFor_ArbitraryLayerName()
            throws Exception {
        String layerName = "TEST";

        ModelSolutionLayer result = ModelSolutionLayerType.modelSolutionLayerFor(layerName);

        assertNotNull(result);
        assertEquals(layerName, result.getSolutionLayerName());

        ModelSolutionLayer result2 = ModelSolutionLayerType.modelSolutionLayerFor(layerName.toLowerCase());
        assertNotNull(result2);
        assertEquals(layerName, result2.getSolutionLayerName());
        assertSame(result, result2);
    }
}