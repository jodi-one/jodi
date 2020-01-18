package one.jodi.base.service.annotation;

import one.jodi.base.service.metadata.DataStoreDescriptor;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Annotation Service defines a low-level service to retrieve annotation meta data
 * associated with a table and column instance. Other types of annotations
 * and annotated objects may be added in the future.<p>
 * <p>
 * The annotations typically describe properties of an object that is not natively
 * defined in the database schema. It can be used to add semantics to tables,
 * columns, fk constraints etc.
 *
 */
public interface AnnotationService {

    Optional<TableAnnotations> getAnnotations(DataStoreDescriptor tableData,
                                              List<Pattern> hiddenColumnPattern);

    List<? extends VariableAnnotations> getVariableAnnotations();

}
