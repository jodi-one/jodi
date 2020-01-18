package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;

public class TestAnnotationFactory implements AnnotationFactory {

    private final ErrorWarningMessageJodi errorWarningMessages;

    public TestAnnotationFactory(final ErrorWarningMessageJodi errorWarningMessages) {
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public TableAnnotations createTableAnnotations(final String schema,
                                                   final String name) {
        return new TestTableAnnotations(schema, name, this, this.errorWarningMessages);
    }

    @Override
    public TableAnnotations createTableAnnotations(final String schema,
                                                   final String name,
                                                   final String businessName,
                                                   final String abbreviatedBusinessName,
                                                   final String description) {
        return new TestTableAnnotations(schema, name, businessName,
                abbreviatedBusinessName, description,
                this, this.errorWarningMessages);
    }

    @Override
    public ColumnAnnotations createColumnAnnotations(final TableAnnotations parent,
                                                     final String name) {
        return new TestColumnAnnotations(parent, name, this.errorWarningMessages);
    }

    @Override
    public ColumnAnnotations createColumnAnnotations(final TableAnnotations parent,
                                                     final String name,
                                                     final String businessName,
                                                     final String abbreviatedBusinessName,
                                                     final String description,
                                                     final Boolean isHidden) {
        return new TestColumnAnnotations(parent, name, businessName,
                abbreviatedBusinessName, description, isHidden,
                this.errorWarningMessages);
    }

    @Override
    public VariableAnnotations createVariableAnnotations(final String name) {
        return new TestVariableAnnotations(name, this.errorWarningMessages);
    }

}
