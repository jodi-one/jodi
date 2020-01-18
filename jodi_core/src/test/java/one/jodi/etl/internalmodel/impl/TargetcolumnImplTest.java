package one.jodi.etl.internalmodel.impl;

import one.jodi.core.executionlocation.ExecutionLocationType;
import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;


public class TargetcolumnImplTest {

    MappingsImpl parent;
    String model = "MODEL";
    String name = "NAME";
    boolean mandatory = true;
    boolean explicitMandatory = false;
    boolean updateKey = true;
    boolean explicitUpdateKey = false;
    boolean insert = true;
    boolean update = true;
    ExecutionLocationtypeEnum executionLocation = ExecutionLocationtypeEnum.TARGET;
    String dataType = "DATATYPE";
    int scale = 1;
    int length = 1;
    boolean useExpression = true;
    int postition = 0;
    ExecutionLocationType explicitExecutionLocationType = null;

    @Before
    public void setup() {
        parent = new MappingsImpl();
        parent.setModel(model);
    }

    @Test
    public void testConstructor() {
        TargetcolumnImpl fixture = new TargetcolumnImpl(
                parent,
                name,
                mandatory,
                explicitMandatory,
                updateKey,
                explicitUpdateKey,
                insert,
                update,
                dataType,
                scale,
                length,
                postition,
                explicitExecutionLocationType);
        assertEquals(parent.getModel(), fixture.getParent().getModel());
        assertEquals(name, fixture.getName());
        assertEquals(mandatory, fixture.isMandatory());
        assertEquals(explicitMandatory, fixture.isExplicitlyMandatory());
        assertEquals(updateKey, fixture.isUpdateKey());
        assertEquals(explicitUpdateKey, fixture.isExplicitlyUpdateKey());
        assertEquals(update, fixture.isUpdate());
        assertEquals(insert, fixture.isInsert());
        assertEquals(0, fixture.getExecutionLocations().size());
        assertEquals(dataType, fixture.getDataType());
        assertEquals(scale, fixture.getScale());
        assertEquals(length, fixture.getLength());
        assert (fixture.getUserDefinedFlags() != null);
    }

    @Test
    public void testSetsAndGets() {
        String flag = "FLAG";
        ArrayList<String> expressions = new ArrayList<String>();
        expressions.add(flag);
        TargetcolumnImpl fixture = new TargetcolumnImpl();
        fixture.addMappingExpressions(expressions);
        fixture.setParent(parent);
        fixture.setName(name);
        fixture.setMandatory(mandatory);
        fixture.setExplicitMandatory(explicitMandatory);
        fixture.setUpdateKey(updateKey);
        fixture.setExplicitUpdateKey(explicitUpdateKey);
        fixture.setInsert(insert);
        fixture.setUpdate(update);
        fixture.setExecutionLocations(Collections.singletonList(executionLocation));
        fixture.setDataType(dataType);
        fixture.setScale(scale);
        fixture.setLength(length);
        assert (fixture.getUserDefinedFlags() != null);


        assertEquals(parent.getModel(), fixture.getParent().getModel());
        assertEquals(name, fixture.getName());
        assertEquals(mandatory, fixture.isMandatory());
        assertEquals(explicitMandatory, fixture.isExplicitlyMandatory());
        assertEquals(updateKey, fixture.isUpdateKey());
        assertEquals(explicitUpdateKey, fixture.isExplicitlyUpdateKey());
        assertEquals(update, fixture.isUpdate());
        assertEquals(insert, fixture.isInsert());
        assertEquals(executionLocation, fixture.getExecutionLocations().get(0));
        assertEquals(dataType, fixture.getDataType());
        assertEquals(scale, fixture.getScale());
        assertEquals(length, fixture.getLength());
        //assert(fixture.getUserDefinedFlags().contains(flag));

        assert (fixture.getMappingExpressions().contains(flag));

    }

}
