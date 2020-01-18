package one.jodi.tools.service.impl;

import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.tools.RepositoryAnalyzer;
import one.jodi.tools.dependency.MappingHolder;
import one.jodi.tools.dependency.MappingProvider;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.finder.IMappingFinder;
import oracle.odi.domain.mapping.physical.MapPhysicalDesign;
import oracle.odi.domain.mapping.physical.MapPhysicalNode;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.finder.IOdiFolderFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public class RepositoryAnalyzerImpl implements RepositoryAnalyzer {
    private final OdiInstance odiInstance;
    private final Logger logger = LogManager.getLogger(RepositoryAnalyzer.class);
    private final JodiProperties properties;
    private final MappingProvider provider;


    @Inject
    public RepositoryAnalyzerImpl(final OdiInstance odiInstance, final JodiProperties properties, final MappingProvider provider) {
        super();
        this.odiInstance = odiInstance;
        this.properties = properties;
        this.provider = provider;
    }


    private void addListItem(HashMap<String, ArrayList<String>> map, String key, String value) {
        ArrayList<String> list = map.get(key);
        if (list == null) {
            list = new ArrayList<String>();
            map.put(key, list);
        }

        list.add(value);
    }

    private HashMap<String, ArrayList<String>> pivot(HashMap<String, ArrayList<String>> in) {
        HashMap<String, ArrayList<String>> out = new HashMap<String, ArrayList<String>>();
        for (String key : in.keySet()) {
            for (String value : in.get(key)) {
                ArrayList<String> resultList = out.get(value);
                if (resultList == null) {
                    resultList = new ArrayList<String>();
                    out.put(value, resultList);
                }
                if (!resultList.contains(key))
                    resultList.add(key);
            }
        }

        return out;
    }


    @Override
    public void printIKMUsage() {
        logger.info("Preparing IKM Usage summary");
        HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();

        IOdiFolderFinder folderFinder = (IOdiFolderFinder) odiInstance.getTransactionalEntityManager().getFinder(OdiFolder.class);

        for (String folderName : provider.getFolderSequence()) {
            OdiFolder folder = folderFinder.findByName(folderName, properties.getProjectCode()).iterator().next();
            for (MappingHolder mappingHolder : provider.getMappingSequence(folderName)) {
                IMappingFinder mappingFinder = (IMappingFinder) odiInstance.getTransactionalEntityManager().getFinder(oracle.odi.domain.mapping.Mapping.class);
                Mapping mapping = mappingFinder.findByName(folder, mappingHolder.getMapping());

                for (MapPhysicalDesign mpd : mapping.getPhysicalDesigns()) {
                    for (MapPhysicalNode mpn : mpd.getPhysicalNodes()) {
                        logger.info("MPN Name = " + mpn.getName());
                        try {
                            if (mpn.isIKMNode()) {
                                addListItem(map, mpn.getIKMName(), mappingHolder.getTarget());
                            }
                        } catch (AdapterException ae) {
                            ae.printStackTrace();
                            logger.warn("Cannot generate IKM stats for mapping " + mappingHolder.getMapping());
                        } catch (MappingException me) {
                            me.printStackTrace();
                            logger.warn("Cannot generate IKM stats for mapping " + mappingHolder.getMapping());
                        }
                    }
                }

            }

            for (String ikm : map.keySet()) {
                logger.info("IKM [" + ikm + "]");
                for (String target : map.get(ikm)) {
                    logger.info("  " + target);
                }
            }

            logger.info("\n\n");
            HashMap<String, ArrayList<String>> pivoted = pivot(map);
            for (String ds : pivoted.keySet()) {
                if (pivoted.get(ds).size() > 1) {
                    logger.info("Datastore '" + ds + "' is used as target for multiple IKMs");
                    for (String ikm : pivoted.get(ds)) {
                        logger.info("  " + ikm);
                    }
                }
            }
        }


    }


    @Override
    public void printLKMUsage() {
        logger.info("Preparing LKM Usage summary");
        HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();

        IOdiFolderFinder folderFinder = (IOdiFolderFinder) odiInstance.getTransactionalEntityManager().getFinder(OdiFolder.class);

        for (String folderName : provider.getFolderSequence()) {
            OdiFolder folder = folderFinder.findByName(folderName, properties.getProjectCode()).iterator().next();
            for (MappingHolder mappingHolder : provider.getMappingSequence(folderName)) {
                IMappingFinder mappingFinder = (IMappingFinder) odiInstance.getTransactionalEntityManager().getFinder(oracle.odi.domain.mapping.Mapping.class);
                Mapping mapping = mappingFinder.findByName(folder, mappingHolder.getMapping());

                for (MapPhysicalDesign mpd : mapping.getPhysicalDesigns()) {
                    for (MapPhysicalNode mpn : mpd.getPhysicalNodes()) {
                        try {
                            if (mpn.isLKMNode()) {
                                for (String sourceName : mappingHolder.getSources()) {
                                    addListItem(map, mpn.getLKMName(), sourceName + " -> " + mappingHolder.getTarget());
                                }
                            }
                        } catch (AdapterException ae) {
                            ae.printStackTrace();
                            logger.warn("Cannot generate LKM stats for mapping " + mappingHolder.getMapping());
                        } catch (MappingException me) {
                            me.printStackTrace();
                            logger.warn("Cannot generate LKM stats for mapping " + mappingHolder.getMapping());
                        }
                    }
                }

            }

            for (String ikm : map.keySet()) {
                logger.info("IKM [" + ikm + "]");
                for (String target : map.get(ikm)) {
                    logger.info("  " + target);
                }
            }
        }


    }


}
