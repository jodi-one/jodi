package one.jodi.base.service.annotation;

public interface AnnotationFactory {

    TableAnnotations createTableAnnotations(final String schema, final String name);

    TableAnnotations createTableAnnotations(final String schema, final String name,
                                            final String businessName,
                                            final String abbreviatedBusinessName,
                                            final String description);

    ColumnAnnotations createColumnAnnotations(final TableAnnotations parent,
                                              final String name);

    ColumnAnnotations createColumnAnnotations(final TableAnnotations parent,
                                              final String name,
                                              final String businessName,
                                              final String abbreviatedBusinessName,
                                              final String description,
                                              final Boolean isHidden);

    VariableAnnotations createVariableAnnotations(final String name);
}
