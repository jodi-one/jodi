package one.jodi.base.tree;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TreeNodeTest {

    private TreeNode<String> fixture;

    private TreeNode<String> createTree(final String[][] nodeDesc) {
        assert (nodeDesc.length > 0);
        TreeNode<String> root = null;
        Map<String, TreeNode<String>> created = new HashMap<>();

        // creates nodes
        for (String[] descriptor : nodeDesc) {
            assert (descriptor.length == 2) : "must be parent name and name";
            assert (descriptor[1] != null || !descriptor[1].isEmpty()) :
                    "node payload must not be null or empty String";
            assert (created.get(descriptor[1]) == null) : "node payload must be unique";
            TreeNode<String> newNode = new TreeNode<>(descriptor[1]);
            created.put(descriptor[1], newNode);
        }

        //creates nodes that are only referenced
        for (String[] descriptor : nodeDesc) {
            if (created.get(descriptor[0]) == null) {
                TreeNode<String> to = new TreeNode<>(descriptor[0]);
                created.put(descriptor[0], to);
            }
        }

        for (String[] descriptor : nodeDesc) {
            TreeNode<String> from = created.get(descriptor[1]);
            if (descriptor[0] != null || !descriptor[0].isEmpty()) {
                TreeNode<String> to = created.get(descriptor[0]);
                to.addChild(from);
//            System.err.println(from.getItem() + " -> " + to.getItem());
            } else {
                root = from;
//            System.err.println("root: " + root);
            }
        }

        if (root == null) {
            // in case a cycle exists with root pick first node defined
            root = created.get(nodeDesc[0][0]);
        }

        return root;
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
    // TESTS
    //

    @Test
    public void testPaths() {
        String[][] graph = {{"Root", "A"}, {"A", "B"},
                {"B", "C1"}, {"C1", "D1"}, {"C1", "D2"},
                {"B", "C2"}, {"C2", "D3"}, {"C2", "D4"}};


        String[][] expected = {{"Root", "A", "B", "C1", "D1"},
                {"Root", "A", "B", "C1", "D2"},
                {"Root", "A", "B", "C2", "D3"},
                {"Root", "A", "B", "C2", "D4"}};


        fixture = createTree(graph);
        List<List<String>> paths = fixture.getPaths();
        // second invocation to test that marks in tree are cleaned up
        paths = fixture.getPaths();
        assertEquals(4, paths.size());
        assertTrue(matches(paths, expected));
    }

    @Test
    public void testReversePaths() {
        String[][] graph = {{"Root", "A"}, {"A", "B"},
                {"B", "C1"}, {"C1", "D1"}, {"C1", "D2"},
                {"B", "C2"}, {"C2", "D3"}, {"C2", "D4"}};


        String[][] expected = {{"D1", "C1", "B", "A", "Root"},
                {"D2", "C1", "B", "A", "Root"},
                {"D3", "C2", "B", "A", "Root"},
                {"D4", "C2", "B", "A", "Root"}};

        fixture = createTree(graph);
        List<List<String>> paths = fixture.getReversePaths();
        // second invocation to test that marks in tree are cleaned up
        assertEquals(4, paths.size());
        assertTrue(matches(paths, expected));
    }

    @Test
    public void testGraphWithCycle() {
        String[][] graph = {{"Root", "A"}, {"A", "B"},
                {"B", "C1"}, {"C1", "D1"}, {"C1", "D2"},
                {"B", "C2"}, {"C2", "D3"}, {"C2", "D4"}};

        fixture = createTree(graph);
        TreeNode<String> a = fixture.getChildren().get(0);
        TreeNode<String> d1 = a.getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(0);
        // loop back to A node
        d1.addChild(a);

        List<String> path = fixture.getCycle();
        assertEquals(4, path.size());

        // repeat check with different start point
        path = d1.getCycle();
        assertEquals(4, path.size());
    }

    @Test
    public void testGraphWithFullCycle() {
        String[][] graph = {{"Root", "A"}, {"A", "B"},
                {"B", "C1"}, {"C1", "D1"}, {"C1", "D2"},
                {"B", "C2"}, {"C2", "D3"}, {"C2", "D4"}};

        fixture = createTree(graph);
        TreeNode<String> d1 = fixture.getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(0);
        // loop back to A node
        d1.addChild(fixture);

        List<String> path = fixture.getCycle();
        assertEquals(5, path.size());
    }

    @Test
    public void testRootWithSelfReference() {
        String[][] graph = {{"Root", "Root"}};
        fixture = createTree(graph);
        List<String> path = fixture.getCycle();
        assertEquals(1, path.size());
    }

    @Test
    public void testGraphWithSelfReference() {
        String[][] graph = {{"Root", "A"}, {"A", "B"},
                {"B", "C1"}, {"C1", "D1"}, {"C1", "D2"},
                {"B", "C2"}, {"C2", "D3"}, {"C2", "D4"}};

        fixture = createTree(graph);
        TreeNode<String> a = fixture.getChildren().get(0);
        // loop back to A node
        a.addChild(a);

        List<String> path = fixture.getCycle();
        assertEquals(1, path.size());
    }

    @Test
    public void testSize() {
        String[][] graph = {{"Root", "A"}, {"A", "B"},
                {"B", "C1"}, {"C1", "D1"}, {"C1", "D2"},
                {"B", "C2"}, {"C2", "D3"}, {"C2", "D4"}};

        fixture = createTree(graph);
        assertEquals(9, fixture.size());
        // second invocation to test that marks in tree are cleaned up
        assertEquals(9, fixture.size());
    }

    @Test
    public void testSizeGraphWithFullCycle() {
        String[][] graph = {{"Root", "A"}, {"A", "B"},
                {"B", "C1"}, {"C1", "D1"}, {"C1", "D2"},
                {"B", "C2"}, {"C2", "D3"}, {"C2", "D4"}};

        fixture = createTree(graph);
        TreeNode<String> d1 = fixture.getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(0)
                .getChildren().get(0);
        // loop back to A node
        d1.addChild(fixture);
        try {
            fixture.size();
            fail("Expected exception");
        } catch (MalformedTreeException e) {
            assertEquals(5, e.getParticipateInCycle().size());
        }
    }

    @Test
    public void testCollectUnique() {
        String[][] graph = {{"Root", "A"}, {"A", "B"},
                {"B", "C1"}, {"C1", "D1"}, {"C1", "D2"},
                {"B", "C2"}, {"C2", "D3"}, {"C2", "D4"}};

        fixture = createTree(graph);
        assertEquals(9, fixture.collectUnique().size());
    }

    @Test
    public void testCollectUniqueWithDuplicates() {
        String[][] graph = {{"Root", "A"}, {"A", "B"},
                {"B", "C1"}, {"C1", "D1"}, {"C1", "D2"},
                {"B", "C2"}, {"C2", "D3"}, {"C2", "D4"}};

        fixture = createTree(graph);
        TreeNode<String> a = fixture.getChildren().get(0);
        fixture.addChild(new TreeNode<>("A"));
        a.addChild(new TreeNode<>("B"));

        assertFalse(fixture.containsCycle());

        assertEquals(9, fixture.collectUnique().size());

        List<List<String>> paths = fixture.getPaths();
        assertEquals(6, paths.size());

    }

    @Test
    public void testTruncatedPathCommon() {
        String[][] graph = {{"Root", "A"}, {"A", "B"},
                {"B", "C1"}, {"C1", "D1"}, {"C1", "D2"},
                {"B", "C2"}, {"C2", "D3"}, {"C2", "D4"}};

        String[][] expected = {{"Root", "A", "B"}};

        fixture = createTree(graph);
        List<List<String>> paths = fixture.getPaths("B");
        assertEquals(1, paths.size());
        assertTrue(matches(paths, expected));
    }

    @Test
    public void testTruncatedPathSubtree() {
        String[][] graph = {{"Root", "A"}, {"A", "B"},
                {"B", "C1"}, {"C1", "D1"}, {"C1", "D2"},
                {"B", "C2"}, {"C2", "D3"}, {"C2", "D4"}};

        String[][] expected = {{"Root", "A", "B", "C1"},
                {"Root", "A", "B", "C2", "D3"},
                {"Root", "A", "B", "C2", "D4"}};
        ;

        fixture = createTree(graph);
        List<List<String>> paths = fixture.getPaths("C1");
        assertEquals(3, paths.size());
        ;
        assertTrue(matches(paths, expected));
    }

}
