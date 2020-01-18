package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.AggregateFunctionEnum;
import one.jodi.etl.internalmodel.OutputAttribute;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PivotImplTest {

    int order = 11;
    String name = "NAME";
    String rowLocator = "ROW LOCATOR";
    AggregateFunctionEnum aggregateFunction = AggregateFunctionEnum.SUM;

    /*
     * public PivotImpl(String name, int order, String rowLocator, AggregateFunctionEnum aggregateFunction) {
        this.name = name;
        this.order = order;
        this.rowLocator = rowLocator;
        this.aggregateFunction = aggregateFunction;
    }
     */
    @Test
    public void testConstructor() {
        PivotImpl fixture = new PivotImpl(name, order, rowLocator, aggregateFunction);
        assertEquals(name, fixture.getName());
        assertEquals(rowLocator, fixture.getRowLocator());
        assertEquals(aggregateFunction, fixture.getAggregateFunction());
        assertEquals(0, fixture.getOutputAttributes().size());
    }

    @Test
    public void testMethods() {
        PivotImpl fixture = new PivotImpl(name, order, rowLocator, aggregateFunction);

        fixture.setName(name);
        fixture.setOrder(order);
        fixture.setAggregateFunction(aggregateFunction);
        OutputAttribute oa = new OutputAttribute() {
            @Override
            public String getName() {
                return "ATTRIBUTE NAME";
            }

            @Override
            public Map<String, String> getExpressions() {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("VALUE", "EXPRESSION");
                return map;
            }

            @Override
            public boolean hasQualifiedExpressions() {
                return true;
            }
        };
        fixture.add(oa);

        assertEquals(name, fixture.getName());
        assertEquals(rowLocator, fixture.getRowLocator());
        assertEquals(aggregateFunction, fixture.getAggregateFunction());
        assertEquals(1, fixture.getOutputAttributes().size());
        assertEquals(oa, fixture.getOutputAttributes().get(0));
    }

}
