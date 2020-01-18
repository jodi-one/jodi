package oracle.odi.domain.project;

import one.jodi.etl.internalmodel.procedure.OptionInternal;
import oracle.odi.domain.project.ProcedureOption.OptionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

public class ProcedureOptionBuilderImpl implements ProcedureOptionBuilder {

    final static Logger logger = LogManager.getLogger(ProcedureOptionBuilderImpl.class);

    @Override
    public void build(OdiUserProcedure userProcedure, OptionInternal optionInternal) {
        ProcedureOption procedureOption = new ProcedureOption(userProcedure, optionInternal.getName());
        // this should be set first;
        procedureOption.setOptionType(mapFrom(optionInternal.getType()));
        if (!optionInternal.getOptionList().isEmpty()) {
            logger.info("Setting options.");
            procedureOption.setChoiceOptionValues(optionInternal.getOptionList().stream().collect(Collectors.toSet()));
        }
        if (optionInternal.getCondition().isPresent()) {
            procedureOption.setConditionExpression(optionInternal.getCondition().get());
        }
        if (optionInternal.getDefaultValue().isPresent() && !optionInternal.getOptionList().isEmpty()) {
            procedureOption.setDefaultValue(optionInternal.getDefaultValue().get(),
                    optionInternal.getOptionList().stream().collect(Collectors.toSet()));
        } else if (optionInternal.getDefaultValue().isPresent()) {
            if (optionInternal.getDefaultValue().get().equals(true) ||
                    optionInternal.getDefaultValue().get().equals(false)
            ) {
                procedureOption.setDefaultValue(Boolean.parseBoolean(optionInternal.getDefaultValue().get().toString().toLowerCase()));
            } else {
                logger.info(String.format("DefaultValue %s", optionInternal.getDefaultValue().get().getClass().getName()));
                procedureOption.setDefaultValue(optionInternal.getDefaultValue().get());
            }
        }
        if (optionInternal.getDescription().isPresent())
            procedureOption.setDescription(optionInternal.getDescription().get());
        if (optionInternal.getHelp().isPresent())
            procedureOption.setHelp(optionInternal.getHelp().get());
        userProcedure.getOptions().add(procedureOption);
    }

    private oracle.odi.domain.project.ProcedureOption.OptionType mapFrom(
            one.jodi.core.procedure.OptionType type) {
        final OptionType optionType;
        switch (type) {
            case BOOLEAN:
                optionType = oracle.odi.domain.project.ProcedureOption.OptionType.CHECKBOX;
                break;
            case VALUE:
            case CHOICE:
                optionType = oracle.odi.domain.project.ProcedureOption.OptionType.CHOICE;
                break;
            default:
            case TEXT:
                optionType = oracle.odi.domain.project.ProcedureOption.OptionType.LONG_TEXT;
                break;
        }
        // BOOLEAN,
        // TEXT,
        // VALUE, // value is from choice
        // CHOICE;
        return optionType;
    }

}
