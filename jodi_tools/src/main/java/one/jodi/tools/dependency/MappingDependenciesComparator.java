package one.jodi.tools.dependency;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to sort Mappings based upon their sources (and lookups).
 */
public class MappingDependenciesComparator implements Comparator<MappingHolder> {
    Map<String, Integer> rankMap = new HashMap<String, Integer>();

    public MappingDependenciesComparator(DirectedGraph<String> directedGraph) {
        rankMap = pivot(directedGraph.topologicalSort());
    }

    private Map<String, Integer> pivot(List<String> list) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (Integer i = 0; i < list.size(); i++) {
            map.put(list.get(i), i);
        }

        return map;
    }

    private int getMaximum(MappingHolder mappingHolder) {
        int max = Integer.MIN_VALUE;
        for (String source : mappingHolder.getSources()) {
            try {
                int i = rankMap.get(source);
                max = i > max ? i : max;
            } catch (NullPointerException npe) {
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
