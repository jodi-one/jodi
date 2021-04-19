package one.jodi.base.tree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TreeNode<T> {
    private static final Logger LOGGER = LogManager.getLogger(TreeNode.class);

    private static final String EXCEPTION_MSG = "Incorrect tree definition. A cycle was detected. ";

    // uses as  a default to get node names in error message
    // by default it is the object name unless the toString method is overwritten
    private static final Function<Object, String> defaultPrintItemName = Object::toString;

    private static final Predicate<Object> defaultFinalNode = it -> false;

    private final Function<Object, String> printItemName;
    private final T item;

    private TreeNode<T> parent = null;
    private final List<TreeNode<T>> children = new ArrayList<>();

    private boolean marked = false;

    public TreeNode(final T item, final Function<Object, String> printItemName) {
        this.item = item;
        this.printItemName = printItemName;
    }

    public TreeNode(final T item) {
        this(item, defaultPrintItemName);
    }

    public T getItem() {
        return this.item;
    }

    public TreeNode<T> getParent() {
        return parent;
    }

    public List<TreeNode<T>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(final TreeNode<T> child) {
        assert (child != null);
        this.children.add(child);
        child.parent = this;
    }

    private MalformedTreeException createException(final List<T> containsCycle, final T duplicate) {

        int firstPosition = containsCycle.indexOf(duplicate);
        assert (firstPosition >= 0);
        final List<T> cycle = containsCycle.subList(firstPosition, containsCycle.size());

        final String path = cycle.stream()
                                 .map(this.printItemName)
                                 .collect(Collectors.joining(", "));
        final String msg = EXCEPTION_MSG + path;
        return new MalformedTreeException(msg, Collections.unmodifiableList(cycle));
    }

    @SuppressWarnings("unchecked")
    public List<T> getCycle() {
        try {
            getPaths();
        } catch (MalformedTreeException e) {
            return (List<T>) e.getParticipateInCycle();
        }
        return Collections.emptyList();
    }

    public boolean containsCycle() {
        return !getCycle().isEmpty();
    }

    private void exceptionOnCycle(final ArrayList<T> currentPath, final int depth) {
        if (this.marked) {
            MalformedTreeException e = createException(currentPath.subList(0, depth), this.item);
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    private int count(final ArrayList<T> currentPath, final int depth) {
        try {
            exceptionOnCycle(currentPath, depth);

            // indicate that this node was visited
            this.marked = true;
            // node itself
            int count = 1;
            // add to current path to support error reporting
            currentPath.add(depth, getItem());

            // process each child node
            for (final TreeNode<T> child : getChildren()) {
                count += child.count(currentPath, depth + 1);
            }
            return count;
        } finally {
            // cleanup mark to prevent false results with next invocation
            this.marked = false;
        }
    }

    public int size() {
        return count(new ArrayList<>(), 0);
    }

    private Set<T> collectUnique(final ArrayList<T> currentPath, final int depth) {
        try {
            exceptionOnCycle(currentPath, depth);
            // indicate that this node was visited
            this.marked = true;

            currentPath.add(depth, getItem());
            Set<T> unique = new LinkedHashSet<>();
            unique.add(getItem());
            // process each child leaf
            for (final TreeNode<T> child : getChildren()) {
                unique.addAll(child.collectUnique(currentPath, depth + 1));
            }
            return unique;
        } finally {
            // cleanup mark to prevent false results with next invocation
            this.marked = false;
        }
    }

    public Set<T> collectUnique() {
        return Collections.unmodifiableSet(collectUnique(new ArrayList<>(), 0));
    }

    private List<List<T>> traverse(final ArrayList<T> currentPath, final int depth,
                                   final Predicate<Object> lastToInclude) {
        try {
            exceptionOnCycle(currentPath, depth);
            // indicate that this node was visited
            this.marked = true;

            final List<List<T>> paths = new ArrayList<>();
            currentPath.add(depth, getItem());
            if (getChildren().isEmpty() || lastToInclude.test(getItem())) {
                // reached leaf or last node to include in path
                paths.add(new ArrayList<>(currentPath.subList(0, depth + 1)));
            } else {
                // process each child leaf
                for (final TreeNode<T> child : getChildren()) {
                    paths.addAll(child.traverse(currentPath, depth + 1, lastToInclude));
                }
            }
            return paths;
        } finally {
            // cleanup mark to prevent false results with next invocation
            this.marked = false;
        }
    }

    public List<List<T>> getPaths() {
        return Collections.unmodifiableList(traverse(new ArrayList<>(), 0, defaultFinalNode));
    }

    public List<List<T>> getReversePaths() {
        List<List<T>> paths = getPaths();
        return Collections.unmodifiableList(paths.stream()
                                                 .peek(Collections::reverse)
                                                 .collect(Collectors.toList()));
    }

    public List<List<T>> getPaths(final T lastToInclude) {
        final Predicate<Object> lastIncludeCond = lastToInclude::equals;
        return Collections.unmodifiableList(traverse(new ArrayList<>(), 0, lastIncludeCond));
    }
}