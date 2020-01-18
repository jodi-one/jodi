package one.jodi.base.model.types;

import one.jodi.base.service.metadata.ForeignReference;

import java.util.List;

public interface LazyCreation {
    List<DataStoreForeignReference> createForeignReferences(
            final List<ForeignReference> foreignRefs,
            final DataStore foreignDataStore);
}
