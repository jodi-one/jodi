package one.jodi.etl.internalmodel.procedure;

import java.util.List;

public interface ProcedureHeader {

    /**
     * @return list of folder and sub-folder names starting with root folder
     */
    List<String> getFolderNames();

    String getFolderPath();

    String getName();

    // path on the file system where the original XML file is located
    String getFilePath();

}
