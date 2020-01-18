package one.jodi.base.factory;

import com.google.inject.AbstractModule;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.base.service.odb.OdbETLProvider;

/**
 * The Class defines the modules required to run when retrieving
 * data from the JDBC connection.
 *
 */
public class OdbModule extends AbstractModule {

    public OdbModule() {
        super();
    }

    @Override
    protected void configure() {
        // Jodi base metadata service
        bind(SchemaMetaDataProvider.class).to(OdbETLProvider.class);
    }

}
