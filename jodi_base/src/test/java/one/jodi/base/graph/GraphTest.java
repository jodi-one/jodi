package one.jodi.base.graph;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class GraphTest {

    private Graph<String> fixture;

    @Before
    public void setUp() throws Exception {
        fixture = new Graph<>();
    }

    @Test
    public void testBasicGraph() {
        Node<String> n1 = fixture.addNode("N1");
        Node<String> n2 = fixture.addNode("N2");
        Node<String> n3 = fixture.addNode("N3");

        fixture.addEdge(n1, n2);
        fixture.addEdge(n1, n3);

        assertEquals(3, fixture.getNodes().size());
        assertEquals("N1", n1.getValue());
        assertTrue(n1.incomingEdges.isEmpty());
        assertEquals(n1, n2.incomingEdges.iterator().next().getFromNode());

        assertEquals(2, n1.outgoingEdges.size());
        assertEquals("N2", n2.getValue());
        assertEquals(1, n2.incomingEdges.size());
        assertEquals("N3", n3.getValue());
        assertEquals(1, n3.incomingEdges.size());

        List<String> sorted = fixture.topologicalSort();
        assertEquals(3, sorted.size());
        assertEquals("N1", sorted.get(0));
        assertTrue("N2".equals(sorted.get(1)) || "N2".equals(sorted.get(2)));
        assertTrue("N3".equals(sorted.get(1)) || "N3".equals(sorted.get(2)));
    }

    @Test
    public void testReverseList() {
        Node<String> n1 = fixture.addNode("N1");
        Node<String> n2 = fixture.addNode("N2");
        Node<String> n3 = fixture.addNode("N3");

        fixture.addEdge(n3, n2);
        fixture.addEdge(n2, n1);

        List<String> sorted = fixture.topologicalSort();
        assertEquals(3, sorted.size());
        assertEquals("N3", sorted.get(0));
        assertEquals("N2", sorted.get(1));
        assertEquals("N1", sorted.get(2));

    }

    @Test
    public void testCyclicGraph() {
        Node<String> n1 = fixture.addNode("N1");
        Node<String> n2 = fixture.addNode("N2");
        Node<String> n3 = fixture.addNode("N3");
        Node<String> n4 = fixture.addNode("N4");

        fixture.addEdge(n1, n2);
        fixture.addEdge(n1, n3);
        fixture.addEdge(n3, n4);
        fixture.addEdge(n4, n3); // cycle!

        try {
            fixture.topologicalSort();
            fail("expected that CycleDetectedException is thrown here.");
        } catch (CycleDetectedException ce) {
            assertEquals(2, ce.getCycleNodes().size());
            assertTrue(ce.getCycleNodes().contains(n3));
            assertTrue(ce.getCycleNodes().contains(n4));
        }
    }

    @Test
    public void groupTablesTree() {
        Node<String> n1 = fixture.addNode("N1");
        Node<String> n2 = fixture.addNode("N2");
        Node<String> n3 = fixture.addNode("N3");
        Node<String> n4 = fixture.addNode("N4");
        Node<String> n5 = fixture.addNode("N5");

        fixture.addEdge(n1, n2);
        fixture.addEdge(n1, n3);
        fixture.addEdge(n2, n4);
        fixture.addEdge(n3, n5);

        List<Set<String>> result = fixture.groupByLongestPredecessorPath();
        assertEquals(3, result.size());
        assertEquals(1, result.get(0).size());
        assertTrue(result.get(0).contains("N1"));
        assertEquals(1, n1.getSupplemental()); // Implementation detail
        assertEquals(2, result.get(1).size());
        assertTrue(result.get(1).contains("N2"));
        assertTrue(result.get(1).contains("N3"));
        assertEquals(2, n2.getSupplemental()); // Implementation detail
        assertEquals(2, n3.getSupplemental()); // Implementation detail
        assertEquals(2, result.get(2).size());
        assertTrue(result.get(2).contains("N4"));
        assertTrue(result.get(2).contains("N5"));
        assertEquals(3, n4.getSupplemental()); // Implementation detail
        assertEquals(3, n5.getSupplemental()); // Implementation detail
    }


    @Test
    public void groupTablesForrest() {
        // tree one
        Node<String> n1 = fixture.addNode("N1");
        Node<String> n2 = fixture.addNode("N2");
        Node<String> n3 = fixture.addNode("N3");
        Node<String> n4 = fixture.addNode("N4");
        Node<String> n5 = fixture.addNode("N5");

        fixture.addEdge(n1, n2);
        fixture.addEdge(n1, n3);
        fixture.addEdge(n2, n4);
        fixture.addEdge(n3, n5);

        // tree two
        Node<String> n11 = fixture.addNode("N11");
        Node<String> n12 = fixture.addNode("N12");
        Node<String> n13 = fixture.addNode("N13");
        Node<String> n14 = fixture.addNode("N14");
        Node<String> n15 = fixture.addNode("N15");

        fixture.addEdge(n11, n12);
        fixture.addEdge(n11, n13);
        fixture.addEdge(n12, n14);
        fixture.addEdge(n13, n15);

        List<Set<String>> result = fixture.groupByLongestPredecessorPath();
        assertEquals(3, result.size());
        assertEquals(2, result.get(0).size());
        assertTrue(result.get(0).contains("N1"));
        assertTrue(result.get(0).contains("N11"));
        assertEquals(4, result.get(1).size());
        assertTrue(result.get(1).contains("N2"));
        assertTrue(result.get(1).contains("N3"));
        assertTrue(result.get(1).contains("N12"));
        assertTrue(result.get(1).contains("N13"));
        assertEquals(4, result.get(2).size());
        assertTrue(result.get(2).contains("N4"));
        assertTrue(result.get(2).contains("N5"));
        assertTrue(result.get(2).contains("N14"));
        assertTrue(result.get(2).contains("N15"));
    }

    @Test
    public void groupTablesGraphWithoutCycle() {
        Node<String> n1 = fixture.addNode("N1");
        Node<String> n2 = fixture.addNode("N2");
        Node<String> n3 = fixture.addNode("N3");
        Node<String> n4 = fixture.addNode("N4");
        Node<String> n5 = fixture.addNode("N5");

        fixture.addEdge(n1, n2);
        fixture.addEdge(n1, n3);
        fixture.addEdge(n1, n5);
        fixture.addEdge(n2, n4);
        fixture.addEdge(n3, n4);
        fixture.addEdge(n4, n5);

        List<Set<String>> result = fixture.groupByLongestPredecessorPath();
        assertEquals(4, result.size());
        assertEquals(1, result.get(0).size());
        assertTrue(result.get(0).contains("N1"));
        assertEquals(2, result.get(1).size());
        assertTrue(result.get(1).contains("N2"));
        assertTrue(result.get(1).contains("N3"));
        assertEquals(1, result.get(2).size());
        assertTrue(result.get(2).contains("N4"));
        assertEquals(1, result.get(3).size());
        assertTrue(result.get(3).contains("N5"));
    }

    @Test(expected = CycleDetectedException.class)
    public void groupTablesGraphWithCycle() {
        Node<String> n1 = fixture.addNode("N1");
        Node<String> n2 = fixture.addNode("N2");
        Node<String> n3 = fixture.addNode("N3");
        Node<String> n4 = fixture.addNode("N4");
        Node<String> n5 = fixture.addNode("N5");

        fixture.addEdge(n1, n2);
        fixture.addEdge(n1, n3);
        fixture.addEdge(n1, n5);
        fixture.addEdge(n2, n4);
        fixture.addEdge(n3, n4);
        fixture.addEdge(n4, n5);
        fixture.addEdge(n5, n1); // introduces cycle

        fixture.groupByLongestPredecessorPath();
    }

}
