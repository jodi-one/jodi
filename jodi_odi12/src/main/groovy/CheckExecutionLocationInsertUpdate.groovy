import oracle.odi.domain.mapping.IMapComponent
import oracle.odi.domain.mapping.MapAttribute
import oracle.odi.domain.mapping.Mapping
import oracle.odi.domain.mapping.finder.IMappingFinder

IMappingFinder mappingsFinder = (IMappingFinder) odiInstance
        .getTransactionalEntityManager().getFinder(Mapping.class);
Set<String> messages = new TreeSet<String>();
Collection<Mapping> mappings = mappingsFinder.findAll();
for (Mapping m : mappings) {
    for (IMapComponent t : m.getTargets()) {
        for (MapAttribute a : t.getAttributes()) {
            String message = "Map '" + m.getName() + "' tc '" + a.getName() + "' insert '" + a.isInsertIndicator() + "' update '" + a.isUpdateIndicator() + "' active '" + a.isActive() + "' check not null '" + a.isCheckNotNull() + "' key '" + a.isKeyIndicator() + "' exec '" + a.getExecuteOnHint() + "'.";
            messages.add(message);
        }
    }
}
for (String m : messages) {
    println m;
}

