package one.jodi.tools.dependency;

import java.util.List;

public interface MappingProvider {

    /**
     * Get list of folders in project ordered by dependencies
     *
     * @return folder names
     */
    public List<String> getFolderSequence();

    /**
     * Get list of mappings for given folder ordered by dependencies.
     *
     * @param folderName foldername
     * @return mappings names
     */
    public List<MappingHolder> getMappingSequence(String folderName);

}
