package one.jodi.core.extensions.strategies;

import java.util.List;

public class KnowledgeModulePropertiesException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final List<String> errors;
    private final String error;

    public KnowledgeModulePropertiesException(List<String> errors) {
        super("");
        this.errors = errors;
        StringBuilder sb = new StringBuilder();
        for (String e : errors) {
            sb.append(e + "   ");
        }
        error = sb.toString();
    }

    @Override
    public String getMessage() {
        return error;
    }

    public List<String> getErrors() {
        return errors;
    }

}
