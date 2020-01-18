package one.jodi.core.service.impl;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class DepthFirstComparatorTest {

    @Test
    public void testSort() {
        String[] paths = {"/root/dir1/dir3/file31.xml",
                "/root/file1.xml",
                "/root/file0.xml",
                "/root/dir2/file41.xml",
                "/root/dir2/file40.xml",
                "/root/dir1/file11.xml",
                "/root/dir1/file10.xml",
                "/root/dir1/dir2/file21.xml",
                "/root/dir1/dir2/file20.xml"};

        String[] expectedPaths = {
                "/root/dir1/dir2/file20.xml",
                "/root/dir1/dir2/file21.xml",
                "/root/dir1/dir3/file31.xml",
                "/root/dir1/file10.xml",
                "/root/dir1/file11.xml",
                "/root/dir2/file40.xml",
                "/root/dir2/file41.xml",
                "/root/file0.xml",
                "/root/file1.xml"
        };

        List<Path> result = Arrays.asList(paths)
                .stream()
                .map(s -> Paths.get(s))
                .collect(Collectors.toMap(p -> p,
                        p -> "some string"))
                .entrySet()
                .stream()
                .sorted(new DepthFirstComparator<String>())
                .map(e -> e.getKey())
                .collect(Collectors.toList());

        int i = 0;
        for (Path path : result) {
            assertEquals(Paths.get(expectedPaths[i++]), path);
        }

    }

    @Test
    public void testCompare() {
        DepthFirstComparator<String> comparator = new DepthFirstComparator<>();

        Map.Entry<Path, String> e1 = new AbstractMap.SimpleEntry<Path, String>(
                Paths.get("root/file1.xml"),
                "some value");
        Map.Entry<Path, String> e2 = new AbstractMap.SimpleEntry<Path, String>(
                Paths.get("root/dir1/file1.xml"),
                "some value");
        assertEquals(1, comparator.compare(e1, e2));
    }

    @Test
    public void testCompareFileinRoot() {
        DepthFirstComparator<String> comparator = new DepthFirstComparator<>();

        Map.Entry<Path, String> e1 = new AbstractMap.SimpleEntry<Path, String>(
                Paths.get("/root/dir1/file1.xml"),
                "some value");
        Map.Entry<Path, String> e2 = new AbstractMap.SimpleEntry<Path, String>(
                Paths.get("/file1.xml"),
                "some value");
        assertEquals(-1, comparator.compare(e1, e2));
    }

}
