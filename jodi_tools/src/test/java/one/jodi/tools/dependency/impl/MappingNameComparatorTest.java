package one.jodi.tools.dependency.impl;

import one.jodi.tools.dependency.MappingHolder;
import one.jodi.tools.dependency.MappingNameComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MappingNameComparatorTest {

    private final static Logger logger = LogManager.getLogger(
            MappingNameComparatorTest.class);

    @Test
    public void testComparator() {
        MappingNameComparator comparator = new MappingNameComparator();
        ArrayList<MappingHolder> list = new ArrayList<MappingHolder>();
        list.add(createMappingHolder("c"));
        list.add(createMappingHolder("a"));
        list.add(createMappingHolder("b"));

        Collections.sort(list, comparator);

        for (MappingHolder mh : list) {
            logger.info(mh.getMapping());
        }

    }

    private MappingHolder createMappingHolder(String name) {
        return new MappingHolder(name, Arrays.asList(""), null, null);
    }
}
