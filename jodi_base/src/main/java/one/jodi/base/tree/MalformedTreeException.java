package one.jodi.base.tree;

import java.util.ArrayList;
import java.util.List;

public class MalformedTreeException extends RuntimeException {

    private static final long serialVersionUID = -3031828073134944994L;

    private final List<Object> participateInCycle;

    public MalformedTreeException(final String message, final List<Object> nodes) {
        super(message);
        this.participateInCycle = new ArrayList<>(nodes);
    }

    public List<Object> getParticipateInCycle() {
        return participateInCycle;
    }
}
