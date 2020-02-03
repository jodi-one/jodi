package one.jodi.core.service.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Comparator;

/**
 * Sort path based on the name of the leading numbers in the file name.
 */
public class DescSequenceComparator implements Comparator<Path> {

    private final static Logger logger =
            LogManager.getLogger(DescSequenceComparator.class);

    private int deriveTransformationSequence(final Path path) {
        int index = TransformationNameHelper.getLeadingInteger(path);
        if (index < 0) {
            logger.error("File " + path.toFile().getName() +
                    " should not have been picked.");
        }
        return index;
    }

    @Override
    public int compare(final Path p1, final Path p2) {
        int i1 = deriveTransformationSequence(p1);
        int i2 = deriveTransformationSequence(p2);
        return (i1 != i2) ? i2 - i1
                : p2.toFile().getName().compareTo(p1.toFile().getName());
    }

}
