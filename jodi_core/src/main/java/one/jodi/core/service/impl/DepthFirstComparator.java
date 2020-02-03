package one.jodi.core.service.impl;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map.Entry;

/**
 * Implements a depth first sorter of Entities with a String key that represents a
 * path from root to file. Folders are separated using the '/' character.
 */
public class DepthFirstComparator<T> implements Comparator<Entry<Path, T>> {

    private final String FOLDER_SEPERATOR;

    public DepthFirstComparator() {
        if (File.separator.equals("\\")) {
            FOLDER_SEPERATOR = "\\\\";
        } else {
            FOLDER_SEPERATOR = File.separator;
        }
    }

    @Override
    public int compare(Entry<Path, T> o1, Entry<Path, T> o2) {
        assert (o1 != null && o1.getKey() != null && o2 != null && o2.getKey() != null);
        String[] path1 = o1.getKey().toString().split(FOLDER_SEPERATOR);
        String[] path2 = o2.getKey().toString().split(FOLDER_SEPERATOR);

        int cmp = 0;
        int i = 0;
        // compare folders
        do {
            // alpha-numerical order for each folder name
            cmp = Integer.signum(path1[i].compareTo(path2[i]));
            i++;
        } while (i < path1.length - 1 && i < path2.length - 1 && cmp == 0);

        if (cmp == 0 && path1.length == path2.length) {
            cmp = Integer.signum(path1[i].compareTo(path2[i]));
        }

        if (cmp == 0) {
            // longer path is 'smaller'
            cmp = Integer.signum(path2.length - path1.length);
        }
        return cmp;
    }

}
