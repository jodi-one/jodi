package one.jodi.core.service.impl;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DescSequenceComparatorTest {

    private final static String ROOT_DIR = File.separator + "root" + File.separator;

    private DescSequenceComparator fixture;

    @Before
    public void setUp() {
        this.fixture = new DescSequenceComparator();
    }

    @Test
    public void testCompareEqualIntegers() {
        Path path1 = Paths.get(ROOT_DIR + "123_SomeTransformationFile.xml");
        Path path2 = Paths.get(ROOT_DIR + "123_Some_Transformation_File.xml");
        assertTrue(fixture.compare(path1, path2) > 0);
    }

    @Test
    public void testCompareEquals() {
        Path path1 = Paths.get(ROOT_DIR + "123_SomeTransformationFile.xml");
        Path path2 = Paths.get(ROOT_DIR + "123_SomeTransformationFile.xml");
        assertEquals(0, fixture.compare(path1, path2));
    }

    @Test
    public void testCompareSmaller() {
        Path path1 = Paths.get(ROOT_DIR + "12_SomeTransformationFile.xml");
        Path path2 = Paths.get(ROOT_DIR + "123_SomeTransformationFile.xml");
        assertTrue(fixture.compare(path1, path2) > 0);
    }

    @Test
    public void testCompareLarger() {
        Path path1 = Paths.get(ROOT_DIR + "123_SomeTransformationFile.xml");
        Path path2 = Paths.get(ROOT_DIR + "12_SomeTransformationFile.xml");
        assertTrue(fixture.compare(path1, path2) < 0);
    }

    @Test
    public void testCompareWithSpecialChars() {
        Path path1 = Paths.get(ROOT_DIR + "123_Some Transformation File #4.xml");
        Path path2 = Paths.get(ROOT_DIR + "12_Some_Transformation_File.xml");
        assertTrue(fixture.compare(path1, path2) < 0);
    }

}
