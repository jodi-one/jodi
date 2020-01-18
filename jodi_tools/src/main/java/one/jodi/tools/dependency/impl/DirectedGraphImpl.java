package one.jodi.tools.dependency.impl;

import one.jodi.tools.dependency.DirectedGraph;

import java.util.*;


public class DirectedGraphImpl<V> implements DirectedGraph<V> {


    private Map<V, List<V>> vertexEdgeMap = new HashMap<V, List<V>>();

    /* (non-Javadoc)
     * @see one.jodi.reverse.core.dependency.DirectedGraph#clear()
     */
    @Override
    public void clear() {
        vertexEdgeMap = new HashMap<V, List<V>>();
    }


    /* (non-Javadoc)
     * @see one.jodi.reverse.core.dependency.DirectedGraph#add(V)
     */
    @Override
    public void add(V vertex) {
        if (vertexEdgeMap.containsKey(vertex)) return;
        vertexEdgeMap.put(vertex, new ArrayList<V>());
    }


    /* (non-Javadoc)
     * @see one.jodi.reverse.core.dependency.DirectedGraph#contains(V)
     */
    @Override
    public boolean contains(V vertex) {
        return vertexEdgeMap.containsKey(vertex);
    }


    /* (non-Javadoc)
     * @see one.jodi.reverse.core.dependency.DirectedGraph#getEdges(V)
     */
    @Override
    public List<V> getEdges(V v) {
        return vertexEdgeMap.get(v);
    }


    /* (non-Javadoc)
     * @see one.jodi.reverse.core.dependency.DirectedGraph#addEdge(V, V)
     */
    @Override
    public void addEdge(V from, V to) {
        this.add(from);
        this.add(to);
        vertexEdgeMap.get(from).add(to);
    }


    /* (non-Javadoc)
     * @see one.jodi.reverse.core.dependency.DirectedGraph#removeEdge(V, V)
     */
    @Override
    public void removeEdge(V from, V to) {
        if (!(this.contains(from) && this.contains(to)))
            throw new IllegalArgumentException("Nonexistent vertex");
        vertexEdgeMap.get(from).remove(to);
    }


    /* (non-Javadoc)
     * @see one.jodi.reverse.core.dependency.DirectedGraph#outDegree()
     */
    @Override
    public Map<V, Integer> outDegree() {
        Map<V, Integer> result = new HashMap<V, Integer>();
        for (V v : vertexEdgeMap.keySet()) result.put(v, vertexEdgeMap.get(v).size());
        return result;
    }

    /* (non-Javadoc)
     * @see one.jodi.reverse.core.dependency.DirectedGraph#inDegree()
     */
    @Override
    public Map<V, Integer> inDegree() {
        Map<V, Integer> degree = new HashMap<V, Integer>();
        for (V v : vertexEdgeMap.keySet()) degree.put(v, 0);
        for (V from : vertexEdgeMap.keySet()) {
            for (V to : vertexEdgeMap.get(from)) {
                degree.put(to, degree.get(to) + 1);
            }
        }
        return degree;
    }


    /* (non-Javadoc)
     * @see one.jodi.reverse.core.dependency.DirectedGraph#topologicalSort()
     */
    @Override
    public List<V> topologicalSort() {
        Map<V, Integer> degree = inDegree();
        LinkedList<V> rootVertices = new LinkedList<V>();
        List<V> result = new ArrayList<V>();

        for (V v : degree.keySet()) {
            if (degree.get(v) == 0) {
                rootVertices.push(v);
            }
        }

        while (!rootVertices.isEmpty()) {
            V v = rootVertices.pop();
            result.add(v);
            for (V neighbor : vertexEdgeMap.get(v)) {
                degree.put(neighbor, degree.get(neighbor) - 1);
                if (degree.get(neighbor) == 0) {
                    rootVertices.push(neighbor);
                }
            }
        }

        return result;
    }

    /* (non-Javadoc)
     * @see one.jodi.reverse.core.dependency.DirectedGraph#containsCycles()
     */
    @Override
    public boolean containsCycles() {
        return topologicalSort().size() != vertexEdgeMap.size();
    }


}
