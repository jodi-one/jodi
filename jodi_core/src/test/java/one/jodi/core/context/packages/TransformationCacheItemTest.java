package one.jodi.core.context.packages;

import org.junit.Test;

import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class TransformationCacheItemTest {

    @Test
    public void test() {
        String name = "NAME";
        Integer packageSequence = 11;
        String packageList = "PACKAGE_LIST";
        TransformationCacheItem item = new TransformationCacheItem(name, packageSequence, packageList, "FOLDER_NAME", false);
        assertEquals(name, item.getName());
        assertEquals(packageSequence, item.getPackageSequence());
    }

    @Test
    public void testCompare() {
        String name = "NAME";
        Integer packageSequence = 11;
        String packageList = "PACKAGE_LIST";
        TransformationCacheItem item1 = new TransformationCacheItem(name, packageSequence, packageList, "FOLDER_NAME", false);
        TransformationCacheItem item2 = new TransformationCacheItem(name, ++packageSequence, packageList, "FOLDER_NAME", false);
        TransformationCacheItem item3 = new TransformationCacheItem(name, ++packageSequence, packageList, "FOLDER_NAME", false);

        assert (item1.compareTo(item2) < 0);
        assert (item1.compareTo(item3) < 0);
        assert (item2.compareTo(item2) == 0);

        TreeSet<TransformationCacheItem> set = new TreeSet<TransformationCacheItem>();
        set.add(item1);
        set.add(item2);
        set.add(item3);

        assert (item1 == set.first());
        assert (item3 == set.last());

    }
}
