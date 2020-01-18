package one.jodi.base.model;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.service.annotation.AnnotationService;
import one.jodi.base.service.metadata.DataModelDescriptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ApplicationBase implements ModelNode {

    private final static Logger logger = LogManager.getLogger(ApplicationBase.class);

    private final static String ERROR_MSG =
            "Schema '%1$s' already exists in application.";

    private final Map<String, SchemaBase> schemas = new HashMap<>();
    private final AnnotationService annotationService;
    private final ErrorWarningMessageJodi errorWarningMessages;

    public ApplicationBase(final AnnotationService annotationService,
                           final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        this.annotationService = annotationService;
        this.errorWarningMessages = errorWarningMessages;
    }

    public String getName() {
        return "root";
    }

    public ModelNode getParent() {
        return null;
    }

    //
    // factory methods
    //

    protected SchemaBase createSchemaInstance(final DataModelDescriptor model,
                                              final DatabaseBase database) {
        return new SchemaBase(this, model.getSchemaName(), model.getDataServerName(),
                model.getModelCode(), database,
                this.annotationService, this.errorWarningMessages);
    }

    protected DatabaseBase createDatabaseInstance(final DataModelDescriptor model) {
        return new DatabaseBase(model.getDataServerName(),
                model.getDataBaseServiceName(),
                model.getDataBaseServicePort(),
                model.getDataServerName());
    }

    //
    //
    //

    public SchemaBase addSchema(final DataModelDescriptor model,
                                final String initExpression)
            throws MalformedModelException {
        SchemaBase schema = createSchemaInstance(model, createDatabaseInstance(model));

        // validate after schema instance has been created that
        // its name does not exist already
        if (schemas.get(schema.getName()) != null) {
            String msg = String.format(ERROR_MSG, model.getSchemaName());
            logger.debug(msg);
            throw new MalformedModelException(schemas.get(model.getSchemaName()), msg);
        }

        schemas.put(model.getSchemaName(), schema);
        return schema;
    }

    public Map<String, ? extends SchemaBase> getSchemaMap() {
        return Collections.unmodifiableMap(this.schemas);
    }

    public List<? extends SchemaBase> getSchemas() {
        return Collections.unmodifiableList(new ArrayList<>(this.schemas.values()));
    }

    public Optional<? extends TableBase> findTable(final String schemaName,
                                                   final String tableName) {
        assert (schemaName != null && !schemaName.trim().isEmpty() &&
                tableName != null && !tableName.trim().isEmpty());
        SchemaBase found = schemas.get(schemaName.trim());
        if (found == null) {
            // schema with the given name does not exist
            return Optional.empty();
        }
        return Optional.ofNullable(found.getTable(tableName.trim()));
    }

}
