package one.jodi.tools.dependency;

import java.util.List;
import java.util.Map;

public interface DirectedGraph<V> {

    public abstract void clear();

    public abstract void add(V vertex);

    public abstract boolean contains(V vertex);

    public abstract List<V> getEdges(V v);

    public abstract void addEdge(V from, V to);

    public abstract void removeEdge(V from, V to);

    public abstract Map<V, Integer> outDegree();

    public abstract Map<V, Integer> inDegree();

    public abstract List<V> topologicalSort();

    public abstract boolean containsCycles();

}