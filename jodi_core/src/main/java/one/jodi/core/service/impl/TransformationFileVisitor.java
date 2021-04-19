package one.jodi.core.service.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.service.files.FileCollectorVisitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TransformationFileVisitor extends FileCollectorVisitor {

    private static final Logger logger =
            LogManager.getLogger(TransformationFileVisitor.class);

    private static final String FILE_EXTENSION = ".xml";

    private static final String ERROR_MESSAGE_00070 =
            "Ignoring file due to error deriving package sequence: %s, %s";

    private final List<String> exclusions = Arrays.asList(
            new String[]{"obiee.xml", "smartexport.xml", "variables.xml",
                    "sequences.xml", "constraints.xml", "procedure.xml",
                    "0.xml", "1.xml"});

    private final List<String> exclusionPrefixes = Arrays.asList(
            new String[]{"variables-", "sequences-", "constraints-", "procedure-"});

    private final List<Path> files = new ArrayList<>();

    private final ErrorWarningMessageJodi errorWarningMessages;

    public TransformationFileVisitor(final ErrorWarningMessageJodi errorWarningMessages) {
        this.errorWarningMessages = errorWarningMessages;
    }

    public List<Path> getPathList() {
        return Collections.unmodifiableList(files);
    }

    private boolean hasExcludedPrefix(final String fileName) {
        return exclusionPrefixes.stream()
                .filter(p -> fileName.startsWith(p))
                .findFirst()
                .isPresent();
    }

    private boolean isExludedName(final Path path) {
        String fileName = path.toFile().getName();
        if (exclusions.contains(fileName.toLowerCase()) ||
                hasExcludedPrefix(fileName.toLowerCase()) ||
                path.toFile().getAbsolutePath().contains("loadPlans") ||
                path.toFile().getAbsolutePath().contains("provided_mappings")
        ) {
            return true;
        }
        return false;
    }

    private boolean hasLeadingInteger(final Path path) {
        try {
            int leading = TransformationNameHelper.getLeadingInteger(path);
            return (leading != -1);
        } catch (NumberFormatException e) {
            String msg = errorWarningMessages.formatMessage(70, ERROR_MESSAGE_00070,
                    this.getClass(),
                    path.toFile().getAbsolutePath(),
                    e.getMessage());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.WARNINGS);
            logger.debug(msg, e);
            return false;
        }
    }

    @Override
    public FileVisitResult visitFile(final Path path,
                                     final BasicFileAttributes attrs)
            throws IOException {
        logger.debug("found file candiate: " + path.toString());
        if (!attrs.isDirectory() && path.toFile().getName().endsWith(FILE_EXTENSION) &&
                !isExludedName(path) && hasLeadingInteger(path)) {
            logger.debug("added file: " + path.toString());
            files.add(path);
        }
        return FileVisitResult.CONTINUE;
    }
}
