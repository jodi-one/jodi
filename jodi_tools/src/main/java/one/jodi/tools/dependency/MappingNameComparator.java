package one.jodi.tools.dependency;

import java.util.Comparator;


/**
 * Class to sort Mappings based upon their sources (and lookups).
 */
public class MappingNameComparator implements Comparator<MappingHolder> {

    public MappingNameComparator() {

    }


    @Override
    public int compare(MappingHolder t1, MappingHolder t2) {

        String name1 = t1.getMapping();
        String name2 = t2.getMapping();

        return name1.compareTo(name2);
    }


}
