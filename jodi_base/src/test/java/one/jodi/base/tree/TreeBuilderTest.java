package one.jodi.base.tree;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TreeBuilderTest {

    private TreeBuilder<String> fixture;

    private Comparator<String> createDefaultComparator() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Comparator<String> cmp = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((String) o1).compareTo((String) o2);
            }
        };
        return cmp;
    }

    private TreeNode<String> createTree(final String[][] paths) {
        assert (paths != null && paths.length > 0);
        List<List<String>> linearPaths = new ArrayList<>();
        for (String[] path : paths) {
            linearPaths.add(Arrays.asList(path));
        }
        return fixture.createTree(linearPaths, createDefaultComparator());
    }

    private TreeNode<String> createTree(final String[][] paths,
                                        final TreeNode<String> root) {
        assert (paths != null && paths.length > 0);
        List<List<String>> linearPaths = new ArrayList<>();
        for (String[] path : paths) {
            linearPaths.add(Arrays.asList(path));
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        Comparator<String> cmp = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((String) o1).compareTo((String) o2);
            }
        };

        return fixture.createTree(root, linearPaths, cmp);
    }

    private boolean matches(final List<String> path,
                            final String[] expectedName) {
        if (path.size() != expectedName.length) return false;
        boolean matched = true;
        for (int i = 0; i < expectedName.length; i++) {
            if (!path.get(i).equals(expectedName[i])) {
                matched = false;
                break;
            }
        }
        return matched;
    }

    private boolean matches(final List<List<String>> paths,
                            final String[][] expectedNames) {
        if (paths.size() != expectedNames.length) return false;
        boolean matched = true;
        for (int i = 0; i < expectedNames.length; i++) {
            if (!matches(paths.get(i), expectedNames[i])) {
                matched = false;
                break;
            }
        }
        return matched;
    }

    //
    //
    //

    @Before
    public void setUp() throws Exception {
        fixture = new TreeBuilder<String>();
    }

    @Test
    public void testOnePath() {
        String[][] inputs = {{"Root", "A", "B", "C1", "D1"}};

        String[][] expected = {{"Root", "A", "B", "C1", "D1"}};

        TreeNode<String> root = createTree(inputs);
        assertEquals(5, root.size());
        List<List<String>> paths = root.getPaths();
        assertEquals(1, paths.size());
        assertTrue(matches(paths, expected));
    }

    @Test
    public void testTwoPaths() {
        String[][] inputs = {{"Root", "A", "B", "C1", "D1"},
                {"Root", "A", "B", "C1", "D2"}};

        String[][] expected = {{"Root", "A", "B", "C1", "D1"},
                {"Root", "A", "B", "C1", "D2"}};

        TreeNode<String> root = createTree(inputs);
        assertEquals(6, root.size());
        List<List<String>> paths = root.getPaths();
        assertEquals(2, paths.size());
        assertTrue(matches(paths, expected));
    }

    @Test
    public void testTwoPathsSameLeaves() {
        String[][] inputs = {{"Root", "A", "B1", "C1", "D1"},
                {"Root", "A", "B2", "C1", "D2"}};

        String[][] expected = {{"Root", "A", "B1", "C1", "D1"},
                {"Root", "A", "B2", "C1", "D2"}};

        TreeNode<String> root = createTree(inputs);
        assertEquals(8, root.size());
        List<List<String>> paths = root.getPaths();
        assertEquals(2, paths.size());
        assertTrue(matches(paths, expected));
    }

    @Test
    public void testSameSubPath() {
        String[][] inputs = {{"Root", "A", "B", "C1", "D1"},
                {"Root", "A", "B", "C1"}};

        String[][] expected = {{"Root", "A", "B", "C1", "D1"}};

        TreeNode<String> root = createTree(inputs);
        assertEquals(5, root.size());
        List<List<String>> paths = root.getPaths();
        assertEquals(1, paths.size());
        assertTrue(matches(paths, expected));
    }

    @Test
    public void testSubPathRootOnly() {
        String[][] inputs = {{"Root", "A", "B", "C1", "D1"},
                {"Root"}};

        String[][] expected = {{"Root", "A", "B", "C1", "D1"}};

        TreeNode<String> root = createTree(inputs);
        assertEquals(5, root.size());
        List<List<String>> paths = root.getPaths();
        assertEquals(1, paths.size());
        assertTrue(matches(paths, expected));
    }

    @Test
    public void testSecondPathWithNull() {
        String[][] inputs = {{"Root", "A", "B", "C1", "D1"},
                {"Root", null}};
        try {
            createTree(inputs);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Null is not an accepted item value.", e.getMessage());
        } catch (RuntimeException er) {
            fail("other exception expected");
        }
    }

    @Test
    public void testSecondPathWithEmptyList() {
        String[][] inputs = {{"Root", "A", "B", "C1", "D1"},
                {}};
        try {
            createTree(inputs);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Null or an empty list are not acceptable " +
                    "inputs for this method.", e.getMessage());
        } catch (RuntimeException er) {
            fail("other exception expected");
        }
    }

    @Test
    public void testDifferentRootItems() {
        String[][] inputs = {{"Root", "A", "B", "C1", "D1"},
                {"Root2"}};
        try {
            createTree(inputs);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("The first node in each list must be identical because " +
                    "it represents the single root node.", e.getMessage());
        } catch (RuntimeException er) {
            fail("other exception expected");
        }
    }

    @Test
    public void testNoRootItems() {
        String[][] inputs = {{}};
        try {
            createTree(inputs);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Null or an empty list are not acceptable inputs " +
                    "for this method.", e.getMessage());
        } catch (RuntimeException er) {
            fail("other exception expected");
        }
    }

    @Test
    public void testTwoPathsRootSupplied() {
        String[][] inputs = {{"A", "B", "C1", "D1"},
                {"A", "B", "C1", "D2"}};

        String[][] expected = {{"Root", "A", "B", "C1", "D1"},
                {"Root", "A", "B", "C1", "D2"}};

        TreeNode<String> root = new TreeNode<String>("Root");
        createTree(inputs, root);
        assertEquals(6, root.size());
        List<List<String>> paths = root.getPaths();
        assertEquals(2, paths.size());
        assertTrue(matches(paths, expected));
    }

    @Test
    public void testNullPaths() {
        try {
            TreeNode<String> root = new TreeNode<String>("Root");
            Comparator<String> cmp = createDefaultComparator();
            fixture.createTree(root, null, cmp);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Null or an empty list are not acceptable inputs " +
                    "for this method.", e.getMessage());
        } catch (RuntimeException er) {
            fail("other exception expected");
        }
    }

    @Test
    public void testNullPath() {
        try {
            TreeNode<String> root = new TreeNode<String>("Root");
            Comparator<String> cmp = createDefaultComparator();
            fixture.createTree(root, Collections.singletonList(null), cmp);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Null is not an accepted item value.", e.getMessage());
        } catch (RuntimeException er) {
            fail("other exception expected");
        }
    }

}
