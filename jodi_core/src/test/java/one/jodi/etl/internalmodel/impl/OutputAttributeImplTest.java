package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.AggregateFunctionEnum;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class OutputAttributeImplTest {

    String name = "NAME";
    AggregateFunctionEnum aggf = AggregateFunctionEnum.SUM;


    @Test
    public void testConstructor() {
        OutputAttributeImpl fixture = new OutputAttributeImpl(name);
        assertEquals(name, fixture.getName());
        assert (fixture.hasQualifiedExpressions() == false);
    }

    @Test
    public void testMethods() {
        OutputAttributeImpl fixture = new OutputAttributeImpl();

        fixture.setName(name);
        String[] values = "VALUE1,VALUE2".split(",");
        String[] expressions = "EXPRESSION1,EXPRESSION2".split(",");
        for (int i = 0; i < values.length; i++) {
            fixture.getExpressions().put(values[i], expressions[i]);
        }

        assertEquals(name, fixture.getName());
        assert (fixture.getExpressions().keySet().containsAll(Arrays.asList(values)));
        assert (fixture.getExpressions().values().containsAll(Arrays.asList(expressions)));
        assert (fixture.hasQualifiedExpressions());
    }

    @Test
    public void testHasNoQualified() {
        OutputAttributeImpl fixture = new OutputAttributeImpl();
        fixture.getExpressions().put(null, "EXPRESSION");
        assert (fixture.hasQualifiedExpressions() == false);
    }


}
