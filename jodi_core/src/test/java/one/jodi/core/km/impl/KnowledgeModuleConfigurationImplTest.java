package one.jodi.core.km.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KnowledgeModuleConfigurationImplTest {

    public static void main(String[] args) {
        new org.junit.runner.JUnitCore().run(KnowledgeModuleConfigurationImplTest.class);
    }

    @Test
    public void testKnowledgeModuleConfiguration() {
        String name = "NAME";
        String optionKey = "OPTIONKEY";
        String optionValue = "OPTIONVALUE";
        KnowledgeModuleConfigurationImpl kmc = new KnowledgeModuleConfigurationImpl();
        kmc.setName(name);
        kmc.putOption(optionKey, optionValue);

        assert (kmc.getName().equals(name));
        assert (kmc.getOptionKeys().contains(optionKey));
        assert (kmc.getOptionValue(optionKey).equals(optionValue));
    }

    @Test
    public void testGetOptionValue() {
        String name = "NAME";
        String optionKey = "OPTIONKEY";
        String optionValue = "OPTIONVALUE";
        KnowledgeModuleConfigurationImpl kmc = new KnowledgeModuleConfigurationImpl();
        kmc.setName(name);
        kmc.putOption(optionKey, optionValue);
        Object result = kmc.getOptionValue(optionKey);
        assertEquals(optionValue, result);
    }
}
