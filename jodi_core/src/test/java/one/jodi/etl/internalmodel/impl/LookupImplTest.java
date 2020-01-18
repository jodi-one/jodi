package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ExecutionLocationtypeEnum;
import one.jodi.etl.internalmodel.LookupTypeEnum;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LookupImplTest {

    SourceImpl parent = new SourceImpl();
    String sourceName = "SOURCENAME";
    String model = "MODEL";
    String lookupDataStore = "LOOKUPDATASTORE";
    String alias = "ALIAS";
    String join = "JOIN";
    LookupTypeEnum lookupType = LookupTypeEnum.LEFT_OUTER;
    ExecutionLocationtypeEnum joinExecutionLocation = ExecutionLocationtypeEnum.SOURCE;
    KmTypeImpl lkm = new KmTypeImpl();
    String lkmName = "LKMNAME";
    boolean temporary = true;
    boolean subSelect = true;
    boolean journalized = false;
    String temporaryDataStore = "TEMPORARYDATASTORE";

    @Before
    public void setup() {
        parent.setName(sourceName);
        lkm.setName(lkmName);
    }

    @Test
    public void testConstructor() {
        LookupImpl fixture = new LookupImpl(parent,
                model,
                lookupDataStore,
                alias,
                join,
                lookupType,
                joinExecutionLocation,
                lkm,
                temporary,
                subSelect,
                journalized);
        assertEquals(parent.getName(), fixture.getParent().getName());
        assertEquals(lookupDataStore, fixture.getLookupDataStore());
        assertEquals(model, fixture.getModel());
        assertEquals(alias, fixture.getAlias());
        assertEquals(join, fixture.getJoin());
        assertEquals(lookupType, fixture.getLookupType());
        assertEquals(joinExecutionLocation, fixture.getJoinExecutionLocation());
        assertEquals(temporary, fixture.isTemporary());
        assertEquals(subSelect, fixture.isSubSelect());
    }

    @Test
    public void testMethods() {
        LookupImpl fixture = new LookupImpl();
        fixture.setParent(parent);
        fixture.setLookupDatastore(lookupDataStore);
        fixture.setModel(model);
        fixture.setAlias(alias);
        fixture.setJoin(join);
        fixture.setLookupType(lookupType);
        fixture.setJoinExecutionLocation(joinExecutionLocation);
        fixture.setTemporary(temporary);
        fixture.setSubSelect(subSelect);

        assertEquals(parent.getName(), fixture.getParent().getName());
        assertEquals(lookupDataStore, fixture.getLookupDataStore());
        assertEquals(model, fixture.getModel());
        assertEquals(alias, fixture.getAlias());
        assertEquals(join, fixture.getJoin());
        assertEquals(lookupType, fixture.getLookupType());
        assertEquals(joinExecutionLocation, fixture.getJoinExecutionLocation());
        assertEquals(temporary, fixture.isTemporary());
        assertEquals(subSelect, fixture.isSubSelect());
    }

}
