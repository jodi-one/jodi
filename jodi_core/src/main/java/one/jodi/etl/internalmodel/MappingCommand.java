package one.jodi.etl.internalmodel;

/**
 * A mapping command is a command that is executed at the beginning or end of a mapping.
 *
 */
public interface MappingCommand {

    /**
     * @return The actual command to be executed.
     */
    String getText();

    /**
     * @return The technology to be used while executing the command retrieved by {@link #getText()}}
     */
    String getTechnology();

    /**
     * @return The logical model to be used while executing the command.
     */
    String getModel();

}
