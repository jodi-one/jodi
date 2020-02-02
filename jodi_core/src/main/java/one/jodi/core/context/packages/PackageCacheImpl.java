package one.jodi.core.context.packages;

import com.google.inject.Inject;
import one.jodi.base.annotations.Registered;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.Register;
import one.jodi.base.util.Resource;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.Transformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The implementation class for the  PackageCache interface.
 *
 */
@Singleton
public class PackageCacheImpl implements PackageCache, Resource {

    private final static Logger logger = LogManager.getLogger(PackageCacheImpl.class);

    private final static String ERROR_MESSAGE_03080 =
            "Attempting to cache transformation with package sequence %d without " +
                    "transformation name set.";

    private final ErrorWarningMessageJodi errorWarningMessages;

    private LinkedHashMap<String, ETLPackage> packageCache;

    // Holds packageList --> (transformation name, packageSequence)
    private HashMap<String, TreeSet<TransformationCacheItem>> associationsToTransformations;

    // Holds PackageListItem --> (package name, creationOrder)
    private HashMap<String, TreeSet<PackageCacheItem>> assocationsToPackages;

    /**
     * Instantiates a new PackageCacheImpl instance.
     *
     * @param registerInstance the register to register this cache with
     * @param errorWarningMessages the link to the error warning messages subsystem
     */
    @Inject
    public PackageCacheImpl(@Registered final Register registerInstance,
                            final ErrorWarningMessageJodi errorWarningMessages) {
        this.errorWarningMessages = errorWarningMessages;
        packageCache = new LinkedHashMap<>();
        associationsToTransformations = new HashMap<>();
        assocationsToPackages = new HashMap<>();
        registerInstance.register(this); // TODO violates bets practices; use Guice injector listener
    }

    @Override
    public void addPackageAssociation(String packageName, String packageListItems,
                                      int creationOrder) {
        List<String> packageList = parsePackageList(packageListItems);

        for (String pl : packageList) {
            TreeSet<PackageCacheItem> list = assocationsToPackages.get(pl);
            if (list == null) {
                list = new TreeSet<>();
                assocationsToPackages.put(pl, list);
            }
            list.add(new PackageCacheItem(packageName, pl, creationOrder));
        }
    }


    @Override
    public List<PackageCacheItem> getPackageNamesForAssociation(String packageListItem) {
        List<PackageCacheItem> result = new ArrayList<>();
        for (String pkg : parsePackageList(packageListItem)) {
            TreeSet<PackageCacheItem> list = assocationsToPackages.get(pkg);
            if (list != null) {
                result.addAll(list);
            }
        }
        return result;
    }

    @Override
    public Set<String> getPackageAssociations() {
        return assocationsToPackages.keySet();
    }


    @Override
    public void addTransformationToPackages(Transformation transformation) {
        List<String> packageList = parsePackageList(transformation.getPackageList());
        String transformationName = transformation.getName();

        if (transformationName == null) {
            String msg = errorWarningMessages.formatMessage(3080,
                    ERROR_MESSAGE_03080, this.getClass(), transformation.getPackageSequence());
            logger.error(msg);

            errorWarningMessages.addMessage(
                    transformation.getPackageSequence(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new PackageCacheException(msg);
        }


        //assert(transformationName != null && transformationName.length() > 0);

        for (String pkg : packageList) {
            TreeSet<TransformationCacheItem> transformationList = associationsToTransformations.get(pkg);
            if (transformationList == null) {
                transformationList = new TreeSet<>();
                associationsToTransformations.put(pkg, transformationList);
            }
            transformationList.add(new TransformationCacheItem(transformation.getName(),
                    transformation.getPackageSequence(),
                    transformation.getPackageList(),
                    transformation.getFolderName(),
                    transformation.isAsynchronous()));
        }

    }

    @Override
    public List<TransformationCacheItem> getTransformationsForPackage(String packageName, String folderCode) {
        List<TransformationCacheItem> result = new ArrayList<>();
        for (String pkg : parsePackageList(packageName)) {
            TreeSet<TransformationCacheItem> list = associationsToTransformations.get(pkg);
            if (list != null) {
                result.addAll(list.stream().filter(cache -> cache.getFolderName()
                        .equals(folderCode))
                        .collect(Collectors.toList()));
            }
        }
        return result;
    }


    /**
     * Parses the package list from a comma separated string to a list of strings.
     *
     * @param packageListString the package list string
     * @return the list of package list items
     */
    private List<String> parsePackageList(String packageListString) {
        List<String> result = new ArrayList<>();
        String[] packageListItems = packageListString.trim().toUpperCase().split("\\s*,\\s*");

        for (String packageListItem : packageListItems) {
            if (packageListItem.trim().length() > 0)
                result.add(packageListItem);
        }
        // I'm keeping the following code as a note as it let the empty package name slip through
        // but I cannot find any need for that yet...
		/*
		if (packageListItems.length>0) {			
			result = Arrays.asList(packageListItems);
		} else {
			result = Collections.emptyList();
		}
		*/

        return result;
    }

    @Override
    public void clear() {
        packageCache.clear();
        associationsToTransformations.clear();
        assocationsToPackages.clear();

    }

    @Override
    public void logStatistics() {
        // ignore for now
    }

    @Override
    public void flush() {
        // ignore for now
    }

}
