package one.jodi.odi12.folder;

import oracle.odi.domain.project.OdiFolder;

public class Odi12FolderHelper {

    public static String getFolderPath(final OdiFolder folder) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        OdiFolder currentFolder = folder;
        assert (currentFolder != null);
        do {
            sb.insert(0, sep);
            sb.insert(0, currentFolder.getName());
            currentFolder = currentFolder.getParentFolder();
            sep = "/";
        } while (currentFolder != null);
        return sb.toString();
    }

}
