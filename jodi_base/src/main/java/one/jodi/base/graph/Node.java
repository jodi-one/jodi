package one.jodi.base.graph;

import java.util.HashSet;
import java.util.Set;

public class Node<T> {
    final Set<Edge<T>> incomingEdges;
    final Set<Edge<T>> outgoingEdges;

    private final T value;

    // supplemental payload; e.g. marker, additional values
    private Object supplemental = null;

    public Node(final T value) {
        this.value = value;
        this.incomingEdges = new HashSet<>();
        this.outgoingEdges = new HashSet<>();
    }

    public T getValue() {
        return value;
    }

    public Object getSupplemental() {
        return this.supplemental;
    }

    public void setSupplemental(final Object supplemental) {
        this.supplemental = supplemental;
    }

    Node<T> addEdge(final Node<T> toNode) {
        Edge<T> edge = new Edge<>(this, toNode);
        this.outgoingEdges.add(edge);
        toNode.incomingEdges.add(edge);
        return this;
    }
}
