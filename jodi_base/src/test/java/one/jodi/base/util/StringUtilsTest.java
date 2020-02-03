package one.jodi.base.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

/**
 * The class <code>StringUtilsTest</code> contains tests for the class
 * <code>{@link StringUtils}</code>.
 */
public class StringUtilsTest {
    /**
     * Perform pre-test initialization.
     *
     * @throws Exception if the initialization fails for some reason
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Perform post-test clean-up.
     *
     * @throws Exception if the clean-up fails for some reason
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Run the boolean containsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testContainsIgnoreCase_NullString() throws Exception {
        String str = null;
        String searchStr = "";

        boolean result = StringUtils.containsIgnoreCase(str, searchStr);

        assertEquals(false, result);
    }

    /**
     * Run the boolean containsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testContainsIgnoreCase_NullSearchString() throws Exception {
        String str = "";
        String searchStr = null;

        boolean result = StringUtils.containsIgnoreCase(str, searchStr);

        assertEquals(false, result);
    }

    /**
     * Run the boolean containsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testContainsIgnoreCase_NoMatch() throws Exception {
        String str = "abcdEFG";
        String searchStr = "HIJ";

        boolean result = StringUtils.containsIgnoreCase(str, searchStr);

        assertEquals(false, result);
    }

    /**
     * Run the boolean containsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testContainsIgnoreCase_LowerSearchUpperString()
            throws Exception {
        String str = "ABCDEFG";
        String searchStr = "cde";

        boolean result = StringUtils.containsIgnoreCase(str, searchStr);

        assertEquals(true, result);
    }

    /**
     * Run the boolean containsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testContainsIgnoreCase_UpperSearchLowerString()
            throws Exception {
        String str = "abcdefg";
        String searchStr = "EFG";

        boolean result = StringUtils.containsIgnoreCase(str, searchStr);

        assertEquals(true, result);
    }

    /**
     * Run the boolean containsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testContainsIgnoreCase_MixedSearchMixedString()
            throws Exception {
        String str = "AbCdEfG";
        String searchStr = "aBc";

        boolean result = StringUtils.containsIgnoreCase(str, searchStr);

        assertEquals(true, result);
    }

    /**
     * Run the boolean containsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testContainsIgnoreCase_EmptySearchEmptyString()
            throws Exception {
        String str = "";
        String searchStr = "";

        boolean result = StringUtils.containsIgnoreCase(str, searchStr);

        assertEquals(true, result);
    }

    /**
     * Run the boolean containsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testContainsIgnoreCase_ExactMatch1() throws Exception {
        String str = "a";
        String searchStr = "a";

        boolean result = StringUtils.containsIgnoreCase(str, searchStr);

        assertEquals(true, result);
    }

    /**
     * Run the boolean containsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testContainsIgnoreCase_ExactMatch2() throws Exception {
        String str = "AbCdEfG";
        String searchStr = "AbCdEfG";

        boolean result = StringUtils.containsIgnoreCase(str, searchStr);

        assertEquals(true, result);
    }

    /**
     * Run the boolean endsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEndsWithIgnoreCase_NullSuffixNullString() throws Exception {
        String str = null;
        String suffix = null;

        boolean result = StringUtils.endsWithIgnoreCase(str, suffix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean endsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEndsWithIgnoreCase_NullSuffix() throws Exception {
        String str = "";
        String suffix = null;

        boolean result = StringUtils.endsWithIgnoreCase(str, suffix);

        assertEquals(false, result);
    }

    /**
     * Run the boolean endsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEndsWithIgnoreCase_NullString() throws Exception {
        String str = null;
        String suffix = "";

        boolean result = StringUtils.endsWithIgnoreCase(str, suffix);

        assertEquals(false, result);
    }

    /**
     * Run the boolean endsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEndsWithIgnoreCase_EmptySuffixEmptyString()
            throws Exception {
        String str = "";
        String suffix = "";

        boolean result = StringUtils.endsWithIgnoreCase(str, suffix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean endsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEndsWithIgnoreCase_ExactMatch1() throws Exception {
        String str = "a";
        String suffix = "a";

        boolean result = StringUtils.endsWithIgnoreCase(str, suffix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean endsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEndsWithIgnoreCase_ExactMatch2() throws Exception {
        String str = "abcdef";
        String suffix = "abcdef";

        boolean result = StringUtils.endsWithIgnoreCase(str, suffix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean endsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEndsWithIgnoreCase_LowerSuffixUpperString()
            throws Exception {
        String str = "ABCDEFG";
        String suffix = "efg";

        boolean result = StringUtils.endsWithIgnoreCase(str, suffix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean endsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEndsWithIgnoreCase_UpperSuffixLowerString()
            throws Exception {
        String str = "abcdefg";
        String suffix = "EFG";

        boolean result = StringUtils.endsWithIgnoreCase(str, suffix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean endsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEndsWithIgnoreCase_MixedSuffixMixedString()
            throws Exception {
        String str = "AbCdEfG";
        String suffix = "eFg";

        boolean result = StringUtils.endsWithIgnoreCase(str, suffix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean endsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEndsWithIgnoreCase_LongerSuffix()
            throws Exception {
        String suffix = "AbCdEfG";
        String str = "eFg";

        boolean result = StringUtils.endsWithIgnoreCase(str, suffix);

        assertEquals(false, result);
    }

    /**
     * Run the boolean equalsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEqualsIgnoreCase_1() throws Exception {
        String str1 = null;
        String str2 = null;

        boolean result = StringUtils.equalsIgnoreCase(str1, str2);

        assertEquals(true, result);
    }

    /**
     * Run the boolean equalsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEqualsIgnoreCase_2() throws Exception {
        String str1 = "";
        String str2 = "";

        boolean result = StringUtils.equalsIgnoreCase(str1, str2);

        assertEquals(true, result);
    }

    /**
     * Run the boolean equalsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEqualsIgnoreCase_NullStr1() throws Exception {
        String str1 = null;
        String str2 = "";

        boolean result = StringUtils.equalsIgnoreCase(str1, str2);

        assertEquals(false, result);
    }

    /**
     * Run the boolean equalsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEqualsIgnoreCase_EmptyStrings() throws Exception {
        String str1 = "";
        String str2 = "";

        boolean result = StringUtils.equalsIgnoreCase(str1, str2);

        assertEquals(true, result);
    }

    /**
     * Run the boolean equalsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEqualsIgnoreCase_LowerStr1UpperStr2() throws Exception {
        String str1 = "abc";
        String str2 = "ABC";

        boolean result = StringUtils.equalsIgnoreCase(str1, str2);

        assertEquals(true, result);
    }

    /**
     * Run the boolean equalsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEqualsIgnoreCase_MixedStr1MixedStr2() throws Exception {
        String str1 = "AbC";
        String str2 = "aBc";

        boolean result = StringUtils.equalsIgnoreCase(str1, str2);

        assertEquals(true, result);
    }

    /**
     * Run the boolean equalsIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testEqualsIgnoreCase_NoMatch() throws Exception {
        String str1 = "abc1";
        String str2 = "ABC";

        boolean result = StringUtils.equalsIgnoreCase(str1, str2);

        assertEquals(false, result);
    }

    /**
     * Run the boolean hasLength(String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testHasLength_1() throws Exception {
        String str = "a";

        boolean result = StringUtils.hasLength(str);

        assertEquals(true, result);
    }

    /**
     * Run the boolean hasLength(String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testHasLength_2() throws Exception {
        String str = "acdfgh";

        boolean result = StringUtils.hasLength(str);

        assertEquals(true, result);
    }

    /**
     * Run the boolean hasLength(String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testHasLength_NullString() throws Exception {
        String str = null;

        boolean result = StringUtils.hasLength(str);

        assertEquals(false, result);
    }

    /**
     * Run the boolean hasLength(String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testHasLength_EmptyString() throws Exception {
        String str = "";

        boolean result = StringUtils.hasLength(str);

        assertEquals(false, result);
    }

    /**
     * Run the boolean isEmpty(String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testIsEmpty_EmptyString() throws Exception {
        String str = "";

        boolean result = StringUtils.isEmpty(str);

        assertEquals(true, result);
    }

    /**
     * Run the boolean isEmpty(String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testIsEmpty_NullString() throws Exception {
        String str = null;

        boolean result = StringUtils.isEmpty(str);

        assertEquals(true, result);
    }

    /**
     * Run the boolean isEmpty(String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testIsEmpty_false1() throws Exception {
        String str = "a";

        boolean result = StringUtils.isEmpty(str);

        assertEquals(false, result);
    }

    /**
     * Run the boolean isEmpty(String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testIsEmpty_false2() throws Exception {
        String str = "abcdfg";

        boolean result = StringUtils.isEmpty(str);

        assertEquals(false, result);
    }

    /**
     * Run the boolean startsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testStartsWithIgnoreCase_NullPrefixNullString()
            throws Exception {
        String str = null;
        String prefix = null;

        boolean result = StringUtils.startsWithIgnoreCase(str, prefix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean startsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testStartsWithIgnoreCase_NullPrefix() throws Exception {
        String str = "";
        String prefix = null;

        boolean result = StringUtils.startsWithIgnoreCase(str, prefix);

        assertEquals(false, result);
    }

    /**
     * Run the boolean startsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testStartsWithIgnoreCase_NullString() throws Exception {
        String str = null;
        String prefix = "";

        boolean result = StringUtils.startsWithIgnoreCase(str, prefix);

        assertEquals(false, result);
    }

    /**
     * Run the boolean startsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testStartsWithIgnoreCase_EmptyPrefixEmptyString()
            throws Exception {
        String str = "";
        String prefix = "";

        boolean result = StringUtils.startsWithIgnoreCase(str, prefix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean startsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testStartsWithIgnoreCase_ExactMatch1() throws Exception {
        String str = "a";
        String prefix = "a";

        boolean result = StringUtils.startsWithIgnoreCase(str, prefix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean startsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testStartsWithIgnoreCase_ExactMatch2() throws Exception {
        String str = "abcdef";
        String prefix = "abcdef";

        boolean result = StringUtils.startsWithIgnoreCase(str, prefix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean startsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testStartsWithIgnoreCase_LowerPrefixUpperString()
            throws Exception {
        String str = "ABCDEFG";
        String prefix = "abc";

        boolean result = StringUtils.startsWithIgnoreCase(str, prefix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean startsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testStartsWithIgnoreCase_UpperPrefixLowerString()
            throws Exception {
        String str = "abcdefg";
        String prefix = "ABC";

        boolean result = StringUtils.startsWithIgnoreCase(str, prefix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean startsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testStartsWithIgnoreCase_MixedPrefixMixedString()
            throws Exception {
        String str = "AbCdEfG";
        String prefix = "aBc";

        boolean result = StringUtils.startsWithIgnoreCase(str, prefix);

        assertEquals(true, result);
    }

    /**
     * Run the boolean startsWithIgnoreCase(String,String) method test.
     *
     * @throws Exception
     */
    @Test
    public void testStartsWithIgnoreCase_LongerPrefix()
            throws Exception {
        String prefix = "AbCdEfG";
        String str = "aBc";

        boolean result = StringUtils.startsWithIgnoreCase(str, prefix);

        assertEquals(false, result);
    }

    @Test
    public void makeReadableTest() {
        assertEquals("", StringUtils.deriveReadableName(""));
        assertEquals("Abc", StringUtils.deriveReadableName("abc"));
        assertEquals("Abc", StringUtils.deriveReadableName("_abc"));
        assertEquals("Abc", StringUtils.deriveReadableName("__abc-"));
        assertEquals("Abc Def", StringUtils.deriveReadableName("abc-def"));
        assertEquals("Abc Def", StringUtils.deriveReadableName("abc-def_"));
        assertEquals("Abc Def", StringUtils.deriveReadableName("abc-def___"));
        assertEquals("Abc Def G", StringUtils.deriveReadableName("abc-def___g"));
    }
}