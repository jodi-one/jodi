package one.jodi.core.context.packages;

import org.junit.Test;

import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class PackageCacheItemTest {

    @Test
    public void test() {
        String name = "NAME";
        int order = 11;
        String packageListItem = "COMMON";
        PackageCacheItem item = new PackageCacheItem(name, packageListItem, order);
        assertEquals(name, item.getName());
        assertEquals(order, item.getCreationOrder());
    }

    @Test
    public void testCompare() {
        String name = "NAME";
        Integer order = 11;
        String packageListItem = "COMMON";
        PackageCacheItem item1 = new PackageCacheItem(name, packageListItem, order);
        PackageCacheItem item2 = new PackageCacheItem(name, packageListItem, ++order);
        PackageCacheItem item3 = new PackageCacheItem(name, packageListItem, ++order);

        assert (item1.compareTo(item2) < 0);
        assert (item1.compareTo(item3) < 0);
        assert (item2.compareTo(item2) == 0);

        TreeSet<PackageCacheItem> set = new TreeSet<PackageCacheItem>();
        set.add(item3);
        set.add(item2);
        set.add(item1);

        assert (item1 == set.first());
        assert (item3 == set.last());

    }

}
