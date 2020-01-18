package one.jodi.etl.internalmodel.impl;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KmTypeImplTest {

    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(KmTypeImplTest.class);
    }

    @Test
    public void testKmType() {
        String name = "NAME";
        String newName = "NEWNAME";
        KmTypeImpl fixture = new KmTypeImpl(name);
        assertEquals(name, fixture.getName());
        fixture.setName(newName);
        assertEquals(newName, fixture.getName());
    }

    @Test
    public void testOptions() {
        String optionKey = "OPTIONKEY";
        String optionValue = "OPTIONVALUE";
        KmTypeImpl fixture = new KmTypeImpl();
        assert (fixture.getOptions() != null);
        fixture.addOption(optionKey, optionValue);
        assertEquals(optionValue, fixture.getOptions().get(optionKey));
        fixture.clearOptions();
        assertEquals(0, fixture.getOptions().size());

    }
}
