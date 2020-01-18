package one.jodi.base.service.annotation;

import java.util.List;
import java.util.Optional;

public interface JsAnnotation {

    Optional<ColumnAnnotations> getColumnAnnotations(TableAnnotations parent,
                                                     String columnName,
                                                     String jsonExpr)
            throws MalformedAnnotationException;

    Optional<TableAnnotations> getTableAnnotations(String schemaName,
                                                   String tableName,
                                                   String jsonExpr)
            throws MalformedAnnotationException;

    Optional<TableAnnotations> getTableAnnotations(Key baseKey)
            throws MalformedAnnotationException;

    List<? extends VariableAnnotations> getVariableAnnotations()
            throws MalformedAnnotationException;

}