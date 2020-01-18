package one.jodi.etl.internalmodel.impl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MappingsImplTest {

    TransformationImpl transformation = new TransformationImpl();
    String transformationName = "X";
    boolean distinct = true;
    String model = "MODEL";
    String stagingModel = "STAGING_MODEL";
    String targetDataStore = "TARGETDATASTORE";
    KmTypeImpl ikm = new KmTypeImpl();
    String ikmName = "IKMNAME";
    KmTypeImpl ckm = new KmTypeImpl();
    String ckmName = "CKMNAME";

    @Before
    public void setup() {
        transformation.setName(transformationName);
        ikm.setName(ikmName);
        ckm.setName(ckmName);
    }

    @Test
    public void testConstructor() {
        MappingsImpl fixture = new MappingsImpl(
                transformation,
                distinct,
                model,
                targetDataStore,
                ikm,
                ckm);
        assertEquals(transformation.getName(), transformationName);
        assertEquals(distinct, fixture.isDistinct());
        assertEquals(model, fixture.getModel());
        assertEquals(targetDataStore, fixture.getTargetDataStore());
        assertEquals(ikmName, ikm.getName());
        assertEquals(ckmName, ckm.getName());
        assertEquals(0, fixture.getTargetColumns().size());
    }

    @Test
    public void testMethods() {
        MappingsImpl fixture = new MappingsImpl();
        assertEquals(0, fixture.getTargetColumns().size());

        fixture.setParent(transformation);
        fixture.setDistinct(distinct);
        fixture.setModel(model);
        fixture.setStagingModel(stagingModel);
        fixture.setTargetDataStore(targetDataStore);
        fixture.setIkm(ikm);
        fixture.setCkm(ckm);
        TargetcolumnImpl targetcolumn = new TargetcolumnImpl();
        fixture.addTargetcolumns(targetcolumn);
        assertEquals(transformation.getName(), transformationName);
        assertEquals(distinct, fixture.isDistinct());
        assertEquals(model, fixture.getModel());
        assertEquals(stagingModel, fixture.getStagingModel());
        assertEquals(targetDataStore, fixture.getTargetDataStore());
        assertEquals(ikmName, ikm.getName());
        assertEquals(ckmName, ckm.getName());
        assertEquals(1, fixture.getTargetColumns().size());
        fixture.clearTargetcolumns();
        assertEquals(0, fixture.getTargetColumns().size());

    }
}
