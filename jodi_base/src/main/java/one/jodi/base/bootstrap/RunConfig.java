package one.jodi.base.bootstrap;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;

import java.util.List;

/**
 * Interface that defines the bootstrap config values for the application.
 *
 */
public interface RunConfig {

    ErrorWarningMessageJodi errorWarningMessages = ErrorWarningMessageJodiImpl.getInstance();

    /**
     * Gets the path to the metadata directory.
     *
     * @return the metadata directory path
     */
    String getMetadataDirectory();

    /**
     * Gets the ModuleProvider class name list.
     *
     * @return the ModuleProvider class name list
     */
    List<String> getModuleClasses();

    /**
     * Gets the path to the application property file.
     *
     * @return the path to the application property file
     */
    String getPropertyFile();

    /**
     * Checks if is dev mode.
     *
     * @return true, if is dev mode
     */
    boolean isDevMode();

    /**
     * Gets the source model
     */
    String getSourceModel();

    /**
     * Gets the target model
     */
    String getTargetModel();

    /**
     * Gets the code for the model of the
     * underlying subsystem.
     *
     * @return ModelCode
     */
    String getModelCode();

    /**
     * Password for the ODI work repository or database schema.
     *
     * @return password for work repository or DB schema
     */
    String getPassword();

    /**
     * Master Password for the ODI master repository.
     *
     * @return password for work repository or DB schema
     */
    String getMasterPassword();

    String getDeploymentArchiveType();
}