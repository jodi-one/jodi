package one.jodi.base.matcher;

/**
 * Implements the
 * <a href="http://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance">
 * Damerau-Levenshtein distance</a>, a.k.a. edit distance.
 */
public class DamerauLevenshteinDistanceImpl implements StringDistanceMeasure {

    private int min(final int a, final int b, final int c) {
        return Math.min(a, Math.min(b, c));
    }

    private int optimalStringAlignmentDistance(final char[] str1,
                                               final char[] str2) {
        int lenStr1 = str1.length;
        int lenStr2 = str2.length;

        int[][] d = new int[lenStr1 + 1][lenStr2 + 1];
        for (int i = 0; i <= lenStr1; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= lenStr2; j++) {
            d[0][j] = j;
        }

        for (int i = 1; i <= lenStr1; i++) {
            for (int j = 1; j <= lenStr2; j++) {
                int cost = (str1[i - 1] == str2[j - 1]) ? 0 : 1;
                d[i][j] = min(d[i - 1][j] + 1,     // deletion
                        d[i][j - 1] + 1,     // insertion
                        d[i - 1][j - 1] + cost); // substitution

                if (i > 1 && j > 1 && str1[i - 1] == str2[j - 2] && str1[i - 2] == str2[j - 1]) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + cost);   // transposition
                }
            }
        }
        return d[lenStr1][lenStr2];
    }

    @Override
    public int distance(final String str1, final String str2) {
        return optimalStringAlignmentDistance(str1.toCharArray(), str2.toCharArray());
    }

}
