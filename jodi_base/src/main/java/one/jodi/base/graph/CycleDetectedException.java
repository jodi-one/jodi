package one.jodi.base.graph;

import java.util.List;

public class CycleDetectedException extends RuntimeException {

    private static final long serialVersionUID = 4727622804003844760L;

    private final List<Node<?>> cycle;

    CycleDetectedException(final String message, final List<Node<?>> cycle) {
        super(message);
        this.cycle = cycle;
    }

    public List<Node<?>> getCycleNodes() {
        return cycle;
    }

}
