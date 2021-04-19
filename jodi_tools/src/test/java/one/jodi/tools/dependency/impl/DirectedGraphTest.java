package one.jodi.tools.dependency.impl;

import one.jodi.tools.dependency.DirectedGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectedGraphTest {

    private static final Logger logger = LogManager.getLogger(DirectedGraphTest.class);

    @Test
    public void test() {
        logger.info("C".compareTo("B"));
    }

    @Test
    public void testAddAndContains() {
        DirectedGraph<String> dg = new DirectedGraphImpl<>();

        dg.addEdge("A", "B");
        dg.add("C");
        assert (dg.outDegree()
                  .get("A") == 1);
        assert (dg.outDegree()
                  .get("C") == 0);

        assert (dg.contains("A"));
        assert (dg.contains("B"));
        assert (dg.contains("C"));
    }

    @Test
    public void testRemove() {
        DirectedGraph<String> dg = new DirectedGraphImpl<>();

        dg.addEdge("A", "B");
        dg.add("C");
        dg.removeEdge("A", "B");

        assert (dg.outDegree()
                  .get("A") == 0);

        assert (dg.contains("A"));
        assert (dg.contains("B"));
        assert (dg.contains("C"));

    }

    @Test
    public void testTopologicalSort() {
        DirectedGraph<String> dg = new DirectedGraphImpl<>();

        dg.addEdge("A", "B");
        dg.addEdge("B", "C");
        dg.addEdge("B", "C");
        dg.addEdge("C", "H");
        dg.addEdge("B", "F");
        dg.addEdge("D", "E");
        dg.addEdge("E", "G");
        dg.addEdge("B", "G");
        dg.add("I");

        List<String> sorted = dg.topologicalSort();
        Map<String, Integer> positions = pivot(sorted);
        assert (positions.get("A") < positions.get("B"));
        assert (positions.get("D") < positions.get("G"));
        //assert(positions.get("D") < positions.get("H"));

    }

    @Test
    public void testCyclic() {
        DirectedGraph<String> dg = new DirectedGraphImpl<>();

        dg.addEdge("A", "B");
        dg.addEdge("B", "C");
        dg.addEdge("C", "A");

        assert (dg.containsCycles());
    }


    private Map<String, Integer> pivot(List<String> list) {
        HashMap<String, Integer> map = new HashMap<>();
        for (Integer i = 0; i < list.size(); i++) {
            map.put(list.get(i), i);
        }

        return map;
    }
}