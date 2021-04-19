package one.jodi.base.util;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.config.JodiConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectXmlObjectsUtilImpl<T, O> implements CollectXmlObjectsUtil<T> {
    private static final Logger LOGGER = LogManager.getLogger(CollectXmlObjectsUtilImpl.class);

    private static final String ERROR_MESSAGE_81150 =
            "Exception while reading definition from file '%1$s': %2$s";

    private final XMLParserUtil<T, O> xmlParserUtil;
    private final String xsdFileName;
    private final ErrorWarningMessageJodi errorWarningMessageJodi;

    public CollectXmlObjectsUtilImpl(final Class<O> objectFactory,
                                     final String xsdFileName,
                                     final ErrorWarningMessageJodi errorWarningMessageJodi) {
        this.xsdFileName = xsdFileName;
        xmlParserUtil = new XMLParserUtil<>(objectFactory,
                JodiConstants.getEmbeddedXSDFileNames(),
                errorWarningMessageJodi);
        this.errorWarningMessageJodi = errorWarningMessageJodi;
    }

    /**
     * Turns an Optional<T> into a Stream<T> of length zero or one depending upon
     * whether a value is present.
     */
    private static <X> Stream<X> toStream(final Optional<X> opt) {
        return opt.map(Stream::of).orElse(Stream.empty());
    }

    private T loadFile(final InputStream inputStream, final String pathName) {
        return xmlParserUtil.loadObjectFromXMLAndValidate(inputStream, this.xsdFileName,
                pathName);
    }

    // protected for test purposes only - to inject an object
    protected Optional<Entry<Path, T>> loadXmlFile(final Path f) {
        try (final InputStream is = new FileInputStream(f.toFile().getAbsolutePath())) {
            return Optional.of(new AbstractMap.SimpleEntry<>(f, loadFile(is, f.toString())));
        } catch (RuntimeException | IOException iox) {
            String msg = this.errorWarningMessageJodi
                    .formatMessage(81150, ERROR_MESSAGE_81150, this.getClass(),
                            f.toFile().getAbsolutePath(),
                            iox.getMessage() != null ? iox.getMessage()
                                    : "");
            errorWarningMessageJodi.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            LOGGER.error(msg, iox);
            iox.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Map<Path, T> collectObjectsFromFiles(final List<Path> files) {
        return files.stream()
                // .filter( e -> !e.toString().contains("provided_mappings"))
                .peek(f -> LOGGER.debug("Processing file: (" +
                        f.toFile().getAbsolutePath() + ")"))
                .flatMap(p -> toStream(loadXmlFile(p)))
                .filter(entry -> !entry.getKey().toString().contains("provided_mappings"))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }
}
