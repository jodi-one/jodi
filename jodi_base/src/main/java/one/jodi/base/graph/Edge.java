package one.jodi.base.graph;

class Edge<T> {
    private final Node<T> fromNode;
    private final Node<T> toNode;

    Edge(final Node<T> fromNode, final Node<T> toNode) {
        this.fromNode = fromNode;
        this.toNode = toNode;
    }

    Node<T> getFromNode() {
        return fromNode;
    }

    Node<T> getToNode() {
        return toNode;
    }

}
