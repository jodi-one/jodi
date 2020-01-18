package one.jodi.core.extensions.strategies;

public class NoKnowledgeModuleFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoKnowledgeModuleFoundException(String message) {
        super(message);
    }
}