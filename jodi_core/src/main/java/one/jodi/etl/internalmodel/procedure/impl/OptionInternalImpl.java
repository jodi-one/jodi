package one.jodi.etl.internalmodel.procedure.impl;

import one.jodi.core.procedure.OptionType;
import one.jodi.etl.internalmodel.procedure.OptionInternal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class OptionInternalImpl implements OptionInternal {

    private final String name;
    private final OptionType optionType;
    private final Optional<Object> defaultValue;
    private final List<String> optionList;
    private final Optional<String> condition;
    private final Optional<String> description;
    private final Optional<String> help;

    OptionInternalImpl(final String name, final OptionType optionType, final Object defaultValue,
                       final List<String> optionList,
                       final String condition, final String description, final String help) {
        this.name = name;
        this.optionType = optionType;
        this.defaultValue = Optional.ofNullable(defaultValue);
        this.optionList = optionList == null || optionList.isEmpty() ? Collections.EMPTY_LIST : optionList;
        this.condition = Optional.ofNullable(condition);
        this.description = Optional.ofNullable(description);
        this.help = Optional.ofNullable(help);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public OptionType getType() {
        return optionType;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return defaultValue;
    }

    @Override
    public List<String> getOptionList() {
        return optionList;
    }

    @Override
    public Optional<String> getCondition() {
        return condition;
    }

    @Override
    public Optional<String> getDescription() {
        return description;
    }

    @Override
    public Optional<String> getHelp() {
        return help;
    }

    @Override
    public String toString() {
        return String.format(
                "Option name: \"%s\", type \"%s\", defaultType \"%s\", condition \"%s\", description \"%s\", help \"%s\".",
                name, optionType.name(), defaultValue.isPresent() ? defaultValue.get().toString() : "",
                condition.isPresent() ? condition.get().toString() : "",
                description.isPresent() ? description.get().toString() : "",
                help.isPresent() ? help.get().toString() : "");
    }
}