package one.jodi.base.service.files;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileCollectorImpl implements FileCollector {

    private static final String ERROR_MESSAGE_00045 = "FATAL: xml file %s not found.";
    private static final String ERROR_MESSAGE_00047 = "FATAL: directory %s not found.";
    private final Logger logger = LogManager.getLogger(FileCollectorImpl.class);
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public FileCollectorImpl(final ErrorWarningMessageJodi errorWarningMessages) {
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public List<Path> collectInPath(final Path path,
                                    final FileCollectorVisitor visitor) {
        try {
            Files.walkFileTree(path, visitor);
        } catch (FileNotFoundException e) {
            String msg = errorWarningMessages.formatMessage(45, ERROR_MESSAGE_00045,
                    this.getClass(),
                    path.toFile().getAbsolutePath());
            logger.error(msg, e);
            throw new FileException(msg, e);
        } catch (IOException e) {
            String msg = errorWarningMessages.formatMessage(47, ERROR_MESSAGE_00047,
                    this.getClass(),
                    path.toFile().getAbsolutePath());
            logger.error(msg, e);
            throw new FileException(msg, e);
        }
        return visitor.getPathList();
    }

    @Override
    public List<Path> collectInPath(final Path path, final String startsWith,
                                    final String endsWith, final String orFilenameIs) {
        XmlFileVisitor visitor = new XmlFileVisitor(startsWith, endsWith, orFilenameIs);
        return collectInPath(path, visitor);
    }

    private static class XmlFileVisitor extends FileCollectorVisitor {
        private final String startsWith;
        private final String endsWith;
        private final String orFilenameIs;
        private final List<Path> files = new ArrayList<>();

        XmlFileVisitor(final String startsWith,
                       final String endsWith, final String orFilenameIs) {
            super();
            this.startsWith = startsWith;
            this.endsWith = endsWith;
            this.orFilenameIs = orFilenameIs;
        }

        @Override
        public List<Path> getPathList() {
            return Collections.unmodifiableList(files);
        }

        @Override
        public FileVisitResult visitFile(final Path file,
                                         final BasicFileAttributes attrs)
                throws IOException {
            if (!attrs.isDirectory() &&
                    (file.toFile().getName().startsWith(startsWith) &&
                            file.toFile().getName().endsWith(endsWith)) ||
                    file.toFile().getName().equals(orFilenameIs)) {
                files.add(file);
            }
            return FileVisitResult.CONTINUE;
        }
    }

}