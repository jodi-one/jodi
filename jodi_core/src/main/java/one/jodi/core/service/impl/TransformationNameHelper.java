package one.jodi.core.service.impl;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformationNameHelper {

    private static final String FILE_EXTENSION = ".xml";
    private static final String REGEX_NAME = "(\\d{2,9})(_[\\S ]+)?" + FILE_EXTENSION;
    private static final Pattern INTF_NAME_PATTERN = Pattern.compile(REGEX_NAME);

    public static int getLeadingInteger(final Path path) throws NumberFormatException {
        Matcher m = INTF_NAME_PATTERN.matcher(path.toFile()
                                                  .getName());
        if (!m.matches()) {
            return -1;
        }
        return Integer.parseInt(m.group(1));
    }

}
