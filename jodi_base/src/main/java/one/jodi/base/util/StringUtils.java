package one.jodi.base.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * The methods in this class were copied from commons-lang-2.6 StringUtils because the ODI library includes a version of commons-lang StringUtils that does not include all of the necessary APIs.
 */
public class StringUtils {
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        int len = searchStr.length();
        int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (str.regionMatches(true, i, searchStr, 0, len)) {
                return true;
            }
        }
        return false;
    }

    public static boolean startsWithIgnoreCase(String str, String prefix) {
        if (str == null || prefix == null) {
            return (str == null && prefix == null);
        }
        if (prefix.length() > str.length()) {
            return false;
        }
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static boolean endsWithIgnoreCase(String str, String suffix) {
        if (str == null || suffix == null) {
            return (str == null && suffix == null);
        }
        if (suffix.length() > str.length()) {
            return false;
        }
        int strOffset = str.length() - suffix.length();
        return str.regionMatches(true, strOffset, suffix, 0, suffix.length());
    }

    public static boolean equalsIgnoreCase(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equalsIgnoreCase(str2);
    }

    public static boolean equals(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equals(str2);
    }

    public static boolean hasLength(String str) {
        return ((str != null) && (str.length() > 0));
    }

    public static boolean isEmpty(String str) {
        return !hasLength(str);
    }

    public static String joinStrings(Collection<String> targetStrings,
                                     String delimiter) {
        return joinStrings(targetStrings, delimiter, "", "");
    }

    public static String joinStrings(Collection<String> targetStrings,
                                     String delimiter, String prefix, String suffix) {
        if (targetStrings == null || targetStrings.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = targetStrings.iterator();
        while (itr.hasNext()) {
            sb.append(prefix).append(itr.next()).append(suffix);

            if (itr.hasNext())
                sb.append(delimiter);
        }

        return sb.toString();
    }

    public static String deriveReadableName(final String text) {
        assert (text.split("\\s").length == 1);
        String[] tokens = text.split("_|-");
        return Arrays.stream(tokens)
                .filter(t -> !t.isEmpty())
                .map(t -> t.substring(0, 1).toUpperCase() +
                        t.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

}
