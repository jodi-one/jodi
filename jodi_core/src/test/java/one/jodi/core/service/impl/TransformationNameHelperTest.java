package one.jodi.core.service.impl;

import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class TransformationNameHelperTest {

    @Test
    public void testGetLeadingIntegerNoXMLPostfix() {
        int result = TransformationNameHelper.getLeadingInteger(
                Paths.get(File.separator + "root" + File.separator +
                        "Transformation234.x"));
        assertEquals(-1, result);
    }

    @Test
    public void testGetLeadingIntegerNoUnderscoreAfterNumber() {
        int result = TransformationNameHelper.getLeadingInteger(
                Paths.get(File.separator + "root" + File.separator +
                        "1234Transformation234.xml"));
        assertEquals(-1, result);
    }

    @Test
    public void testGetLeadingIntegerNoNumber() {
        int result = TransformationNameHelper.getLeadingInteger(
                Paths.get(File.separator + "root" + File.separator +
                        "Transformation234.xml"));
        assertEquals(-1, result);
    }

    @Test
    public void testGetLeadingInteger1Number() {
        int result = TransformationNameHelper.getLeadingInteger(
                Paths.get(File.separator + "root" + File.separator +
                        "1_Transformation234.xml"));
        assertEquals(-1, result);
    }

    @Test
    public void testGetLeadingInteger2Numbers() {
        int result = TransformationNameHelper.getLeadingInteger(
                Paths.get(File.separator + "root" + File.separator +
                        "12_Transformation234.xml"));
        assertEquals(12, result);
    }

    @Test
    public void testGetLeadingInteger9Numbers() {
        int result = TransformationNameHelper.getLeadingInteger(
                Paths.get("123456789_Transformation234.xml"));
        assertEquals(123456789, result);
    }

    @Test
    public void testGetLeadingInteger10Numbers() {
        int result = TransformationNameHelper.getLeadingInteger(
                Paths.get("1234567890_Transformation234.xml"));
        assertEquals(-1, result);
    }

}
