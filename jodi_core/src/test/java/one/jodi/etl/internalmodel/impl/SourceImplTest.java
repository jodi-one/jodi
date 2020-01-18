package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.JoinTypeEnum;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SourceImplTest {

    DatasetImpl parent = new DatasetImpl();
    String parentName = "DATASET";
    String model = "MODEL";
    String name = "NAME";
    String alias = "ALIAS";
    String filter = "FILTER";
    ExecutionLocationtypeEnum filterExecutionLocation = ExecutionLocationtypeEnum.SOURCE;
    String join = "JOIN";
    JoinTypeEnum joinType = JoinTypeEnum.CROSS;
    ExecutionLocationtypeEnum joinExecutionLocation = ExecutionLocationtypeEnum.TARGET;
    boolean isSubSelect = true;
    boolean isTemporary = true;
    boolean journalized = false;
    String temporaryDataStore = "TEMPORARYDATASTORE";


    @Before
    public void setup() {
        parent.setName(parentName);
    }

    @Test
    public void testConstructor() {
        SourceImpl fixture = new SourceImpl(parent,
                model,
                name,
                alias,
                filter,
                filterExecutionLocation,
                join,
                joinType,
                joinExecutionLocation,
                isSubSelect,
                isTemporary,
                journalized);

        assertEquals(parentName, fixture.getParent().getName());
        assertEquals(model, fixture.getModel());
        assertEquals(name, fixture.getName());
        assertEquals(alias, fixture.getAlias());
        assertEquals(filter, fixture.getFilter());
        assertEquals(filterExecutionLocation, fixture.getFilterExecutionLocation());
        assertEquals(join, fixture.getJoin());
        assertEquals(joinType, fixture.getJoinType());
        assertEquals(joinExecutionLocation, fixture.getJoinExecutionLocation());
        assertEquals(isSubSelect, fixture.isSubSelect());
        assertEquals(isTemporary, fixture.isTemporary());
        assertEquals(0, fixture.getLookups().size());
        //assertEquals(temporaryDataStore, fixture.getTemporaryDataStore());
    }

    @Test
    public void testMethods() {
        SourceImpl fixture = new SourceImpl();
        fixture.setParent(parent);
        fixture.setModel(model);
        fixture.setAlias(alias);
        fixture.setName(name);
        fixture.setFilterExecutionLocation(filterExecutionLocation);
        fixture.setFilter(filter);
        fixture.setJoin(join);
        fixture.setJoinExecutionLocation(joinExecutionLocation);
        fixture.setJoinType(joinType);
        fixture.setSubSelect(isSubSelect);
        fixture.setTemporary(isTemporary);
        //fixture.setTemporaryDataStore(temporaryDataStore);


        assertEquals(parentName, fixture.getParent().getName());
        assertEquals(model, fixture.getModel());
        assertEquals(name, fixture.getName());
        assertEquals(alias, fixture.getAlias());
        assertEquals(filter, fixture.getFilter());
        assertEquals(filterExecutionLocation, fixture.getFilterExecutionLocation());
        assertEquals(join, fixture.getJoin());
        assertEquals(joinType, fixture.getJoinType());
        assertEquals(joinExecutionLocation, fixture.getJoinExecutionLocation());
        assertEquals(isSubSelect, fixture.isSubSelect());
        assertEquals(isTemporary, fixture.isTemporary());
        assertEquals(0, fixture.getLookups().size());
        //assertEquals(temporaryDataStore, fixture.getTemporaryDataStore());

        LookupImpl lookup = new LookupImpl();
        fixture.addLookup(lookup);
        assertEquals(1, fixture.getLookups().size());


    }

}
