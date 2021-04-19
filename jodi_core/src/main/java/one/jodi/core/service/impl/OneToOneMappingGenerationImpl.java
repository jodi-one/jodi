package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.service.ExtractionTables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Implements the {@link OneToOneMappingGeneration}interface.
 */
public class OneToOneMappingGenerationImpl implements ExtractionTables {

    private static final String ERROR_MESSAGE_03251 = "The ODI target data "
            + "model %1$s is not declared in the Jodi properties file.";

    private static final Logger logger = LogManager.getLogger(OneToOneMappingGenerationImpl.class);

    private final DatabaseMetadataService databaseMetadataService;
    private final ModelPropertiesProvider modelPropertiesProvider;
    private final ErrorWarningMessageJodi errorWarningMessages;

    protected int packageSequence = 0;

    @Inject
    public OneToOneMappingGenerationImpl(final DatabaseMetadataService
                                                 databaseMetadataService, final ModelPropertiesProvider
                                                 modelPropertiesProvider,
                                         final ErrorWarningMessageJodi errorWarningMessages) {
        this.databaseMetadataService = databaseMetadataService;
        this.modelPropertiesProvider = modelPropertiesProvider;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public void genExtractTables(String sourceModel, String targetModel,
                                 int packageSequenceStart, String xmlOutputDir) {
        this.packageSequence = packageSequenceStart;

        Map<String, DataStore> datastores = databaseMetadataService
                .getAllDataStoresInModel(sourceModel);

        for (DataStore datastore : datastores.values()) {
            generateFile(datastore, sourceModel, targetModel,
                    modelPropertiesProvider, this.packageSequence, xmlOutputDir);
            this.packageSequence += 5;
            logger.debug("Interface XML created for " + datastore.getDataStoreName());
        }
    }

    /**
     * Method to create the code
     *
     * @param modelPropertiesProvider
     * @param modelString
     * @return
     */
    private String getModelCode(final ModelPropertiesProvider modelPropertiesProvider,
                                final String modelString) {
        String code = null;

        List<ModelProperties> modelProperties = modelPropertiesProvider.
                getConfiguredModels();

        for (ModelProperties mp : modelProperties) {
            if (mp.getModelID() != null) {
                if (mp.getCode().trim().equals(modelString)) {
                    code = mp.getModelID() + ".code";
                    break;
                }
            }
        }
        return code;
    }

    /**
     * Generate xml files based on the sourceModel, targetModel, packageSequence,
     * and into the xmlOutputDir.
     *
     * @param dataStore
     * @param sourceModel
     * @param targetModel
     * @param modelPropertiesProvider
     * @param pkgSequence
     * @param xmlOutputDir
     */
    private void generateFile(final DataStore dataStore, final String sourceModel,
                              final String targetModel, final ModelPropertiesProvider
                                      modelPropertiesProvider, final int pkgSequence, final String
                                      xmlOutputDir) {
        BufferedWriter out = null;
        try {
            File output = new File(xmlOutputDir, this.packageSequence + "_"
                    + (dataStore.getDataStoreName().replace("#", "*"))
                    + ".xml");

            OutputStreamWriter fstream;

            fstream = new OutputStreamWriter(new FileOutputStream(output),
                    Charset.forName("UTF-8"));
            out = new BufferedWriter(fstream);

            String targetModelString = "";

            if ((targetModel != null) && (targetModel.length() > 0)) {

                String code = getModelCode(modelPropertiesProvider, targetModel);

                if (code == null) {
                    fstream.close();
                    String msg = errorWarningMessages.formatMessage(3251,
                            ERROR_MESSAGE_03251, this.getClass(), targetModel);
                    errorWarningMessages.addMessage(
                            pkgSequence, msg,
                            MESSAGE_TYPE.ERRORS);
                    logger.error(msg);
                    throw new RuntimeException(msg);
                }
                targetModelString = "       <Model>" + code + "</Model>\n";
            }

            String code = getModelCode(modelPropertiesProvider, sourceModel);

            if (code == null) {
                fstream.close();
                String msg = errorWarningMessages.formatMessage(3251,
                        ERROR_MESSAGE_03251, this.getClass(), sourceModel);
                errorWarningMessages.addMessage(
                        pkgSequence, msg,
                        MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            // TODO - replace reference to PACKAGE_ALL with lookup to
            // properties? This should be parameterized.
            String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                    + "<Transformation xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "             xsi:noNamespaceSchemaLocation=\"../../../../../../jodi_core/src/main/resources/jodi-packages.v1.1.xsd\">\n"
                    + "    <PackageList>PACKAGE_ALL</PackageList>\n"
                    + "    <Datasets>\n"
                    + "      <Model>"
                    + code
                    + "</Model>\n"
                    + "      <Dataset>\n"
                    + "        <Source>\n"
                    + "            <Name>"
                    + dataStore.getDataStoreName().trim()
                    + "</Name>\n"
                    + "            <Alias>"
                    + dataStore.getDataStoreName().trim().replace("$", "")
                    + "</Alias>\n"
                    + "        </Source>\n"
                    + "      </Dataset>\n"
                    + "    </Datasets>\n"
                    + "    <Mappings>\n"
                    + targetModelString
                    + "       <TargetDataStore>"
                    + dataStore.getDataStoreName().trim()
                    + "</TargetDataStore>\n"
                    + "    </Mappings>\n"
                    + "</Transformation>";
            out.write(content);
            out.flush();
        } catch (FileNotFoundException e) {
            logger.error("Cannot find file ", e);
        } catch (IOException e) {
            logger.error("Cannot load file ", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error("Cannot close file ", e);
                }
            }
        }
    }
}
