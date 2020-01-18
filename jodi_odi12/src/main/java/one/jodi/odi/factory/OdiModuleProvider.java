package one.jodi.odi.factory;

import com.google.inject.Module;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.factory.ModuleProvider;
import one.jodi.odi.common.OdiVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OdiModuleProvider implements ModuleProvider {

    private static final Logger logger = LogManager.getLogger(OdiModuleProvider.class);
    private static final String ERROR_MESSAGE_01001 =
            "ODI 12.1.2 unsupported by Jodi";
    private static final String ERROR_MESSAGE_01002 =
            "Class %s was not found in classpath";
    private final ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiImpl.getInstance();

    @SuppressWarnings("unchecked")
    @Override
    public List<? extends Module> getModules(final RunConfig config) {
        errorWarningMessages.setMetaDataDirectory(config.getMetadataDirectory());
        List<Module> modules = new ArrayList<>();
        OdiModule core = new OdiModule(config);
        modules.add(core);
        String odiModuleClassName = "one.jodi.odi12.factory.Odi12Module";
        try {
            if (new OdiVersion().isVersion11()) {
                odiModuleClassName = "one.jodi.odi11.factory.Odi11Module";
            } else if (new OdiVersion().isVersion1213() || new OdiVersion().isVersion122()) {
                odiModuleClassName = "one.jodi.odi12.factory.Odi12Module";
            } else if (new OdiVersion().isVersion1212()) {
                String msg = errorWarningMessages.formatMessage(1001,
                        ERROR_MESSAGE_01001, this.getClass());
                logger.error(msg);
                throw new UnRecoverableException(msg);
            } else {
                String msg = "ODI provider not found. Please review and " +
                        "modify you classpath";
                logger.error(msg);
                throw new UnRecoverableException(msg);
            }
        } catch (Exception e) {
            logger.info("Can't determine Odiversion defaulting to ODI12.");
        }
        if (odiModuleClassName != null) {
            Module odiImplModule;
            try {
                Class<? extends Module> moduleClass = (Class<? extends Module>) Class
                        .forName(odiModuleClassName);
                odiImplModule = moduleClass.getDeclaredConstructor()
                        .newInstance();
                modules.add(odiImplModule);
            } catch (final Exception e) {
                String msg = errorWarningMessages.formatMessage(1002,
                        ERROR_MESSAGE_01002, this.getClass(),
                        odiModuleClassName);
                logger.error(msg, e);
                throw new UnRecoverableException(msg, e);
            }
        }

        return Collections.unmodifiableList(modules);
    }

    @Override
    public List<? extends Module> getOverrideModules(RunConfig config) {
        return Collections.emptyList();
    }

}
