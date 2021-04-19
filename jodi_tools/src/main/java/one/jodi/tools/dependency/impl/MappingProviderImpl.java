package one.jodi.tools.dependency.impl;

import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.tools.dependency.MappingDependenciesComparator;
import one.jodi.tools.dependency.MappingHolder;
import one.jodi.tools.dependency.MappingNameComparator;
import one.jodi.tools.dependency.MappingProvider;
import one.jodi.tools.dependency.MappingType;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.domain.mapping.exception.MapComponentException;
import oracle.odi.domain.mapping.exception.MapConnectionException;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.finder.IMappingFinder;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.finder.IOdiFolderFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MappingProviderImpl implements MappingProvider {
    public static final String SortMappingsOptionName = "tools.mapping_sort_policy";
    public static final String SortMappingsByDependency = "DEPENDENCY";
    public static final String SortMappingsByName = "NAME";
    public static final String SortMappingsDefault = "DEFAULT";
    public static final String FilterMappingsRegex = "tools.filter_mapping_regex";
    private final OdiInstance odiInstance;
    private final JodiProperties properties;
    private final Logger logger = LogManager.getLogger(MappingProviderImpl.class);
    LinkedHashMap<String, List<MappingHolder>> linkedMap = new LinkedHashMap<>();
    Pattern filterPattern = null;
    private final DirectedGraphImpl<String> dependencies = new DirectedGraphImpl<>();


    @Inject
    public MappingProviderImpl(final OdiInstance odiInstance, final JodiProperties properties) {
        super();
        this.odiInstance = odiInstance;
        this.properties = properties;

        if (properties.getPropertyKeys()
                      .contains(FilterMappingsRegex)) {
            String value = properties.getProperty(FilterMappingsRegex);
            try {
                filterPattern = Pattern.compile(value);
            } catch (PatternSyntaxException pse) {
                logger.error("Jodi properties option '" + FilterMappingsRegex +
                                     "' is an invalid regular expression.  Ignoring for generation.");
                filterPattern = null;
            }
        }
    }

    @Override
    public List<String> getFolderSequence() {
        if (linkedMap.isEmpty()) {
            try {
                build();
            } catch (MappingException e) {
                e.printStackTrace();
            }

        }

        ArrayList<String> list = new ArrayList<>();
        for (String s : linkedMap.keySet()) {
            list.add(s);
        }
        return list;
    }


    @Override
    public List<MappingHolder> getMappingSequence(String folderName) {
        if (linkedMap.isEmpty()) {
            try {
                build();
            } catch (MappingException e) {
                e.printStackTrace();
            }

        }

        return linkedMap.get(folderName);
    }


    private void build() throws MappingException {

        IOdiFolderFinder folderFinder = (IOdiFolderFinder) odiInstance.getTransactionalEntityManager()
                                                                      .getFinder(OdiFolder.class);
        Collection<OdiFolder> folders = folderFinder.findByProject(properties.getProjectCode());
        for (OdiFolder folder : folders) {
            IMappingFinder mappingFinder = (IMappingFinder) odiInstance.getTransactionalEntityManager()
                                                                       .getFinder(
                                                                               oracle.odi.domain.mapping.Mapping.class);
            Collection<Mapping> mappings = mappingFinder.findByProject(properties.getProjectCode(), folder.getName());

            ArrayList<MappingHolder> mappingList = new ArrayList<>();
            linkedMap.put(folder.getName(), mappingList);

            for (Mapping mapping : mappings) {
                if (mapping.getTargets()
                           .size() > 1) {
                    logger.warn("Cannot generate Jodi specification for Mapping " + folder.getName() + "." +
                                        mapping.getName() + " - it contains multiple targets.");
                    continue;
                } else if (!filterMapping(mapping)) {
                    continue;
                }

                DatastoreComponent targetComponent = null;
                if (mapping.getTargets()
                           .size() > 0 && mapping.getTargets()
                                                 .get(0) instanceof DatastoreComponent) {
                    targetComponent = (DatastoreComponent) mapping.getTargets()
                                                                  .get(0);
                } else {
                    logger.warn("Cannot generate Jodi specification for Mapping " + folder.getName() + "." +
                                        mapping.getName() + " - it contains a target that is not a DataStoreComponent");
                    continue;
                }
                boolean sourcesOK = true;

                for (IMapComponent sourceIMapComponent : mapping.getSources()) {
                    if (!(sourceIMapComponent instanceof DatastoreComponent)) {
                        sourcesOK = false;
                        logger.warn("Cannot generate Jodi specification for Mapping " + folder.getName() + "." +
                                            mapping.getName() + " - it contains a source '" +
                                            sourceIMapComponent.getName() + "' that is not a DataStoreComponent");
                    }
                }

                if (!sourcesOK) {
                    continue;
                }

                ArrayList<String> sourceDataStores = new ArrayList<>();
                for (IMapComponent sourceIMapComponent : mapping.getSources()) {
                    DatastoreComponent sourceComponent = (DatastoreComponent) sourceIMapComponent;
                    String from = getDatastore(sourceComponent);
                    String to = getDatastore(targetComponent);

                    dependencies.addEdge(from, to);

                    sourceDataStores.add(getDatastore(sourceComponent));
                }

                MappingType mappingType = getMappingType(mapping);
                if (mappingType == MappingType.Indeterminate) {
                    logger.error(mapping.getName() +
                                         " does not contain a flow in a form that Jodi can reverse engineer and will be ignored.");
                } else {
                    mappingList.add(
                            new MappingHolder(mapping.getName(), sourceDataStores, getDatastore(targetComponent),
                                              mappingType));
                }
            }

        }

        sortMappings();
		/*
		MappingDependenciesComparator comparator = new MappingDependenciesComparator(dependencies);
		for(String folder : linkedMap.keySet()) {
			Collections.sort(linkedMap.get(folder), comparator);
		}
		*/

    }

    private boolean filterMapping(Mapping mapping) {
        return filterPattern != null ? filterPattern.matcher(mapping.getName())
                                                    .matches() : true;
    }


    private void sortMappings() {

        if (properties.getPropertyKeys()
                      .contains(SortMappingsOptionName)) {
            String value = properties.getProperty(SortMappingsOptionName);
            if (SortMappingsByDependency.equals(value)) {
                logger.info("Attempting to sort ODI Mappings by dependency");
                MappingDependenciesComparator comparator = new MappingDependenciesComparator(dependencies);
                for (String folder : linkedMap.keySet()) {
                    Collections.sort(linkedMap.get(folder), comparator);
                }
            } else if (SortMappingsByName.equals(value)) {
                logger.info("Attempting to sort ODI Mappings by lexicographic order");
                MappingNameComparator comparator = new MappingNameComparator();
                for (String folder : linkedMap.keySet()) {
                    Collections.sort(linkedMap.get(folder), comparator);
                }
            } else if (SortMappingsDefault.equals(value)) {
                logger.info("Applying no sort order to ODI Mappings.");
            } else {
                logger.error("Invalid value '" + value + "' referenced in properties file for tools option '" +
                                     SortMappingsOptionName + "'.");
            }
        } else {
            logger.info("Applying no sort order to ODI Mappings.");
        }
    }


    private String getDatastore(DatastoreComponent datastoreComponent) {
        try {
            return datastoreComponent.getBoundDataStore()
                                     .getQualifiedName();

        } catch (MapComponentException e) {
            throw new RuntimeException(e);
        }
    }


    private MappingType getMappingType(Mapping mapping) throws MapConnectionException, MappingException {
        MappingType rval = MappingType.Indeterminate;
        for (MappingType mappingType : MappingType.values()) {
            if (mappingType == MappingType.Indeterminate) {
                continue;
            }
            String[] types = Arrays.copyOfRange(mappingType.getValue(), 1, mappingType.getValue().length);
            if (evaluateUpstreamComponents(mapping.getTargets()
                                                  .get(0), types)) {
                rval = mappingType;
            }
        }

        return rval;
    }

    private boolean justUpstream(IMapComponent component, String typeName) throws MapConnectionException,
            MappingException {
        boolean b = !component.getUpstreamConnectedLeafComponents()
                              .isEmpty();
        for (IMapComponent upstreamComponent : component.getUpstreamConnectedLeafComponents()) {
            b &= typeName.equals(upstreamComponent.getComponentTypeName());
        }

        return b;
    }

    private boolean evaluateUpstreamComponents(IMapComponent component, String... types) throws MapConnectionException,
            MappingException {

        //boolean rval = types.isEmpty() ? false : justUpstream(component, types.pop());

        if (types.length == 0) {
            return true;
        }

        boolean rval = justUpstream(component, types[0]);
        types = Arrays.copyOfRange(types, 1, types.length);
        if (rval) {
            for (IMapComponent upstreamComponent : component.getUpstreamConnectedLeafComponents()) {
                rval &= evaluateUpstreamComponents(upstreamComponent, types);
            }
        }
        return rval;

    }

}

	/*
	private DataStore getDatastore(DatastoreComponent datastoreComponent) {
		try {		
			String modelCode = datastoreComponent.getBoundDataStore().getModel().getName().toUpperCase();
			String name = datastoreComponent.getName().toUpperCase();
			
			Map<String, DataStore> datastores = databaseMetadataService.getAllDataStoresInModel(modelCode);
			
			DataStore datastore =  datastores.get(modelCode + "." + name);
			if(datastore == null) {
				logger.warn("Cannot find datastore for " + datastoreComponent.getBoundDataStore().getModel().getName() + "." + datastoreComponent.getName());
			}
			
			return datastore;
			
		} catch (MapComponentException e) {
			e.printStackTrace();
		} catch (AdapterException e) {
			e.printStackTrace();
		}
		
		throw new RuntimeException ("Cannot get Jodi datastore " + datastoreComponent.getName());
	}

	*/
	
	
	


/*
 * package one.jodi.reverse.core.dependency;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.jodi.core.metadata.types.DataStore;
import one.jodi.core.model.Dataset;
import one.jodi.core.model.Source;
import one.jodi.core.model.Transformation;
import oracle.odi.domain.mapping.exception.MapComponentException;


public class MappingDependenciesComparator implements Comparator<MappingHolder> {
	Map<DataStore, Integer> rankMap = new HashMap<DataStore, Integer>();
	
	public MappingDependenciesComparator(DirectedGraph<DataStore> directedGraph) {
		rankMap = pivot(directedGraph.topologicalSort());
	}
	
	private Map<DataStore, Integer> pivot(List<DataStore> list) {
		HashMap<DataStore, Integer> map = new HashMap<DataStore, Integer>();
		for(Integer i = 0; i < list.size(); i++) {
			map.put(list.get(i), i);
		}
		
		return map;
	}
	
	private int getMaximum(MappingHolder mappingHolder) {
		int max = Integer.MIN_VALUE;
		for(DataStore source : mappingHolder.getSources()) {
			try {
				int i = rankMap.get(source);
				max = i > max ? i : max;
			}
			catch(NullPointerException npe) {
				System.err.println("cannot find source in dependencies: " + source);
			}
		}
		
		return max;
	}
	
	
	@Override
	public int compare(MappingHolder t1, MappingHolder t2) {
		Integer max1 = getMaximum(t1);
		Integer max2 = getMaximum(t2);

		return max1.compareTo(max2);
	}
	
	
}
*/
 

