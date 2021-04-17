package one.jodi.base.graph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Graph<T> {

    private final static Logger logger = LogManager.getLogger(Graph.class);

    // please note that graph is destroyed after topological sort has completed
    private final Set<Node<T>> allNodes = new HashSet<>();

    public Set<Node<T>> getNodes() {
        return Collections.unmodifiableSet(this.allNodes);
    }

    public Node<T> addNode(final T value) {
        Node<T> newNode = new Node<>(value);
        this.allNodes.add(newNode);
        return newNode;
    }

    public void addEdge(final Node<T> fromNode, final Node<T> toNode) {
        assert (this.allNodes.contains(fromNode) && this.allNodes.contains(toNode));
        fromNode.addEdge(toNode);
    }

    public List<T> topologicalSort(
            final BiFunction<Node<T>, Node<T>, Integer> determineSupplement) {
        // topologically sorted graph nodes (referenced as "L" in literature)
        List<Node<T>> topologicallySorted = new ArrayList<>();

        // Set of all nodes with no incoming edges (referred to as "S" in literature)
        Set<Node<T>> noIncomingEdges = this.allNodes
                .stream()
                .filter(n -> n.incomingEdges.size() == 0)
                .collect(Collectors.toSet());

        // while nodes without incoming edges exist do
        while (!noIncomingEdges.isEmpty()) {
            // remove a node from S
            Node<T> node = noIncomingEdges.iterator().next();
            noIncomingEdges.remove(node);
            // insert n into topologically sorted list
            topologicallySorted.add(node);

            // for each node m with an edge e from n to m do
            // this must be done using the iterator because we remove item from
            // collection and only this feature supports this action without
            // creating a concurrentModificationException
            for (Iterator<Edge<T>> it = node.outgoingEdges.iterator(); it.hasNext(); ) {
                // remove edge e from the graph
                Edge<T> e = it.next();
                Node<T> m = e.getToNode();
                it.remove();// Remove edge from n
                m.incomingEdges.remove(e);// Remove edge from m

                m.setSupplemental(determineSupplement.apply(node, m));

                // if m has no other incoming edges then insert m into S
                if (m.incomingEdges.isEmpty()) {
                    noIncomingEdges.add(m);
                }
            }
        }

        // Check to see if all edges are removed; otherwise report cycle
        List<Node<?>> inCycle = this.allNodes
                .stream()
                .filter(n -> !n.incomingEdges.isEmpty())
                .collect(Collectors.toList());

        if (!inCycle.isEmpty()) {
            String msg = "Cycle detected in graph.";
            logger.error(msg);
            throw new CycleDetectedException(msg, inCycle);
        }

        return topologicallySorted.stream()
                .map(Node::getValue)
                .collect(Collectors.toList());
    }

    public List<T> topologicalSort() {
        return topologicalSort((Node<T> parent, Node<T> child) -> null);
    }


    public List<Set<T>> groupByLongestPredecessorPath() {
        // set level to 1 for all nodes
        this.allNodes.forEach(n -> n.setSupplemental(1));

        BiFunction<Node<T>, Node<T>, Integer> determineChildLevel =
                (Node<T> parent, Node<T> child)
                        -> Math.max(((Integer) parent.getSupplemental()) + 1,
                        (Integer) child.getSupplemental());
        topologicalSort(determineChildLevel);

        // group nodes by level and order in ascending order by level
        return this.allNodes
                .stream()
                .collect(Collectors.groupingBy(n -> (Integer) n.getSupplemental(),
                        TreeMap::new,
                        Collectors.mapping(Node::getValue,
                                Collectors.toSet())))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        Collections::unmodifiableList));
    }

}
