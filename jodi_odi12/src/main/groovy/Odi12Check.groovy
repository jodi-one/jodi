import oracle.odi.domain.mapping.Mapping
import oracle.odi.domain.mapping.finder.IMappingFinder

import java.text.SimpleDateFormat
import java.util.Map.Entry

TreeMap<Date, Mapping> sorted = new TreeMap<Date, Mapping>();
IMappingFinder mappingsFinder = (IMappingFinder) odiInstace
        .getTransactionalEntityManager().getFinder(Mapping.class);
Collection<Mapping> mappings = mappingsFinder.findAll();
for (final Mapping mapping : mappings) {
    sorted.put(mapping.getLastDate(), mapping);
}
SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDD-HH24mm");
for (Entry<Date, Mapping> map : sorted.entrySet()) {
    println sdf.format(map.getKey()) + " mapping: " + map.getValue().getName()
}