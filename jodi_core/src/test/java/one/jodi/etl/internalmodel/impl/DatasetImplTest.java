package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.SetOperatorTypeEnum;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DatasetImplTest {
    /*
     * Transformation parent,
                String name,
                SetOperatorTypeEnum setOperator
     */
    TransformationImpl parent = new TransformationImpl();
    String transformationName = "TRANSFORMATIONNAME";
    String name = "NAME";
    SetOperatorTypeEnum setOperator = SetOperatorTypeEnum.UNION;

    @Before
    public void setup() {
        parent.setName(transformationName);
    }

    @Test
    public void testConstructor() {
        DatasetImpl fixture = new DatasetImpl(parent, name, setOperator);
        assertEquals(parent.getName(), fixture.getParent().getName());
        assertEquals(name, fixture.getName());
        assertEquals(setOperator, fixture.getSetOperator());
        assertEquals(0, fixture.getSources().size());
    }

    @Test
    public void testMethods() {
        DatasetImpl fixture = new DatasetImpl();
        fixture.setName(name);
        fixture.setParent(parent);
        fixture.setSetOperator(setOperator);
        assertEquals(0, fixture.getSources().size());

        SourceImpl source = new SourceImpl();
        fixture.addSource(source);
        assertEquals(1, fixture.getSources().size());

        fixture.clearSources();
        assertEquals(0, fixture.getSources().size());

    }

}
