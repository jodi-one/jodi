package one.jodi.base.bootstrap;


/**
 * The Interface implemented by action handlers.
 *
 */
public interface ActionRunner {

    /**
     * Run the application based on the provided run configuration.
     *
     * @param config the config
     */
    void run(RunConfig config);

    /**
     * Validate the configuration based on the desired action.
     *
     * @param config the config
     * @throws UsageException the usage exception
     */
    void validateRunConfig(RunConfig config) throws UsageException;
}
