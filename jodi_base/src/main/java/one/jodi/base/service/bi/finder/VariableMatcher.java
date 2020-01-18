package one.jodi.base.service.bi.finder;

import java.util.Optional;

public interface VariableMatcher {
    /**
     * @param variableName name of variable for which we try to determine if it exists
     * @return type of variable if it exists and Optional.empty() if no variable
     * exists
     */
    Optional<String> existsVariable(String variableName);
}
