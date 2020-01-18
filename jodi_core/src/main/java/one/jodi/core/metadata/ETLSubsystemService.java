package one.jodi.core.metadata;

import one.jodi.core.metadata.types.EtlStrategyDescriptor;
import one.jodi.core.metadata.types.KnowledgeModule;

import java.util.List;

public interface ETLSubsystemService {

    List<KnowledgeModule> getKMs();

    /**
     * Wrapper method to obtain the list of  ODI Knowledge Modules that are loaded in an ODI
     * project with project name wired in Jodi properties file.
     *
     * @return list of OdiKM<?> names that are available for given project
     */
    List<EtlStrategyDescriptor> getKMstrategy();
}
