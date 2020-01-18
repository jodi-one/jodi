package one.jodi.odi12.mappings;

import oracle.odi.domain.mapping.MapRootContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public class CacheMap extends LinkedHashMap<String, MapRootContainer> {

    private static final Logger logger = LogManager.getLogger(CacheMap.class);
    private static final long serialVersionUID = 1L;
    private int maxEntries;

    @Override
    protected boolean removeEldestEntry(final Map.Entry<String, MapRootContainer> eldest) {
        boolean removeEldest = size() > maxEntries;
        if (removeEldest) {
            logger.debug("The CacheMap has removed the oldest entry.");
        }
        return removeEldest;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int mAX_ENTRIES) {
        maxEntries = mAX_ENTRIES;
    }
}
