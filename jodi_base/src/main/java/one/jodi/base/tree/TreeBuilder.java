package one.jodi.base.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TreeBuilder<T> {

    private final static String ONE_LIST_REQUIRED =
            "Null or an empty list are not acceptable inputs for this method.";

    private final static String ONE_ROOT_REQUIRED =
            "The first node in each list must be identical because " +
                    "it represents the single root node.";

    private final static String ROOT_NO_NULL_ALLOWED =
            "Null is not an accepted root value.";

    private final static String NO_NULL_ALLOWED =
            "Null is not an accepted item value.";

    private Optional<TreeNode<T>> matchingChild(final TreeNode<T> parent,
                                                final T next,
                                                final Comparator<T> comparator) {
        return parent.getChildren()
                .stream()
                .filter(c -> comparator.compare(c.getItem(), next) == 0)
                .findFirst();
    }


    /**
     * Creates a tree by accepting lists that each start at root and continue to
     * a leaf node. Identical sequences from nodes attached to root to leaves
     * will be aligned with the existing tree structure.
     *
     * @param root       node all other roots will be attached
     * @param linear     List that contains node values attached from root to leaf
     * @param comparator comparator to determine if two nodes are identical
     * @return root of the tree
     * @throws IllegalArgumentException
     */
    public TreeNode<T> createTree(final TreeNode<T> root,
                                  final List<List<T>> linearPaths,
                                  final Comparator<T> comparator)
            throws IllegalArgumentException {
        if (root == null) {
            throw new IllegalArgumentException(ROOT_NO_NULL_ALLOWED);
        } else if (linearPaths == null) {
            throw new IllegalArgumentException(ONE_LIST_REQUIRED);
        }

        for (final List<T> path : linearPaths) {
            if (path == null) {
                throw new IllegalArgumentException(NO_NULL_ALLOWED);
            }
            TreeNode<T> current = root;
            for (final T item : path) {
                if (item == null) {
                    throw new IllegalArgumentException(NO_NULL_ALLOWED);
                }
                Optional<TreeNode<T>> matching = matchingChild(current, item,
                        comparator);
                if (matching.isPresent()) {
                    current = matching.get();
                } else {
                    TreeNode<T> newChild = new TreeNode<>(item);
                    current.addChild(newChild);
                    current = newChild;
                }
            }
        }
        return root;
    }

    /**
     * Creates a tree by accepting lists that each start at root and continue to
     * a leaf node. Identical sequences from root to leaves will be aligned with
     * the existing tree structure.
     *
     * @param linear     List that contains node values attached from root to leaf
     * @param comparator comparator to determine if two nodes are identical
     * @return root of the tree
     * @throws IllegalArgumentException
     */
    public TreeNode<T> createTree(final List<List<T>> linearPaths,
                                  final Comparator<T> comparator)
            throws IllegalArgumentException {
        if (linearPaths == null || linearPaths.isEmpty() ||
                linearPaths.get(0).isEmpty() || linearPaths.get(0).get(0) == null) {
            throw new IllegalArgumentException(ONE_LIST_REQUIRED);
        }

        List<List<T>> prunedPath = new ArrayList<>();
        final TreeNode<T> root = new TreeNode<T>(linearPaths.get(0).get(0));

        for (List<T> path : linearPaths) {
            // validate root nodes and prune lists
            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException(ONE_LIST_REQUIRED);
            } else if (comparator.compare(path.get(0), root.getItem()) != 0) {
                throw new IllegalArgumentException(ONE_ROOT_REQUIRED);
            }
            prunedPath.add(path.subList(1, path.size()));
        }

        return createTree(root, prunedPath, comparator);
    }
}
