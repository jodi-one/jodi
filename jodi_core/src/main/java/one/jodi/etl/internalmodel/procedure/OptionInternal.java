package one.jodi.etl.internalmodel.procedure;

import one.jodi.core.procedure.OptionType;

import java.util.List;
import java.util.Optional;

public interface OptionInternal {

    String getName();

    OptionType getType();

    Optional<Object> getDefaultValue();

    List<String> getOptionList();

    Optional<String> getCondition();

    Optional<String> getDescription();

    Optional<String> getHelp();

}
