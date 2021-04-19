package one.jodi.base.service.annotation;

import com.google.inject.Inject;
import one.jodi.base.config.BaseConfigurations;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.service.metadata.ColumnMetaData;
import one.jodi.base.service.metadata.DataStoreDescriptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationServiceDefaultImpl implements AnnotationService {

    private static final Logger logger = LogManager.getLogger(AnnotationServiceDefaultImpl.class);

    private static final String ERROR_MESSAGE_80300 =
            "Malformed annotation file prevents access to annotations of table '%1$s'.";

    private static final String ERROR_MESSAGE_80310 = "Programming error when accessing table '%1$s' annotation.";

    private static final String ERROR_MESSAGE_80320 = "Malformed embedded annotation in table '%1$s' of schema '%2$s'.";

    private static final String ERROR_MESSAGE_80330 =
            "Malformed embedded annotation in column '%1$s' of table '%2$s' in " + "schema '%3$s'.";

    private final TableBusinessRules businessRules;
    private final JsAnnotation jsAnnotation;
    private final KeyParser keyParser;
    private final AnnotationFactory annotationFactory;
    private final BaseConfigurations config;
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public AnnotationServiceDefaultImpl(final TableBusinessRules businessRules, final JsAnnotation jsAnnotation,
                                        final KeyParser keyParser, final AnnotationFactory annotationFactory,
                                        final BaseConfigurations config,
                                        final ErrorWarningMessageJodi errorWarningMessages) {
        this.businessRules = businessRules;
        this.jsAnnotation = jsAnnotation;
        this.keyParser = keyParser;
        this.annotationFactory = annotationFactory;
        this.config = config;
        this.errorWarningMessages = errorWarningMessages;
    }

    private StringBuilder getBaseKey(final DataStoreDescriptor dataStore) {
        StringBuilder sb = new StringBuilder(13);
        sb.append(KeyParser.NS_SCHEMA)
          .append(".");
        sb.append(dataStore.getDataModelDescriptor()
                           .getSchemaName())
          .append(".");
        sb.append(KeyParser.NS_TABLE)
          .append(".");
        sb.append(dataStore.getDataStoreName());
        return sb;
    }

    //
    // Implement Bulk API
    //

    private boolean isHiddenByDefault(final ColumnMetaData column, final List<Pattern> hiddenColumnPattern) {
        return hiddenColumnPattern.stream()
                                  .anyMatch(p -> p.matcher(column.getName())
                                                  .matches());
    }

    private boolean throwAnnotationException() {
        return !config.continueOnAnnotationFailure();
    }

    private Optional<ColumnAnnotations> getColumnAnnotations(final TableAnnotations parent, final ColumnMetaData column,
                                                             final DataStoreDescriptor tableData,
                                                             final List<Pattern> hiddenColumnPattern) {
        Optional<ColumnAnnotations> result = Optional.empty();

        String businessName = businessRules.getExtendedMetadata(column);
        String abbrBusinessName = businessRules.getAbbreviatedMetadata(column);
        String description = businessRules.getDescription(column);
        boolean isHiddenByDefault = isHiddenByDefault(column, hiddenColumnPattern);
        if (!businessName.trim()
                         .isEmpty() || !abbrBusinessName.trim()
                                                        .isEmpty() || !description.trim()
                                                                                  .isEmpty() || isHiddenByDefault) {
            // create Annotation object with default annotations
            ColumnAnnotations ca = annotationFactory.createColumnAnnotations(parent, column.getName(),
                                                                             businessName.trim()
                                                                                         .isEmpty() ? null
                                                                                                    : businessName.trim(),
                                                                             abbrBusinessName.trim()
                                                                                             .isEmpty() ? null
                                                                                                        : abbrBusinessName.trim(),
                                                                             description.trim()
                                                                                        .isEmpty() ? null
                                                                                                   : description.trim(),
                                                                             isHiddenByDefault ? true : null);
            result = Optional.of(ca);
        }

        // retrieve JSON-based meta data embedded in table comments
        String jsonExpr = businessRules.getMetadata(column);
        Optional<ColumnAnnotations> embedded = Optional.empty();
        if (!jsonExpr.isEmpty()) {
            try {
                // retrieve from annotation in data store
                embedded = this.jsAnnotation.getColumnAnnotations(parent, column.getName(), jsonExpr);
            } catch (MalformedAnnotationException e) {
                String msg = errorWarningMessages.formatMessage(80330, ERROR_MESSAGE_80330, this.getClass(),
                                                                column.getName(), tableData.getDataStoreName(),
                                                                tableData.getDataModelDescriptor()
                                                                         .getSchemaName());
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
                logger.fatal(msg);
                if (throwAnnotationException()) {
                    throw new UnRecoverableException(msg, e);
                }
            }
        }

        if (result.isPresent() && embedded.isPresent()) {
            result.get()
                  .merge(embedded.get());
        } else if (embedded.isPresent()) {
            result = embedded;
        }

        // retrieve from annotations.js file
        // JS-file-based annotations were processed beforehand and passed
        // into this method with the parent tableAnnotations
        Optional<ColumnAnnotations> fileBased = Optional.ofNullable(parent.getColumnAnnotations()
                                                                          .get(column.getName()));

        if (result.isPresent() && fileBased.isPresent()) {
            result.get()
                  .merge(fileBased.get());
        } else if (fileBased.isPresent()) {
            result = fileBased;
        }

        return result;
    }

    private List<ColumnAnnotations> getColumnAnnotations(final TableAnnotations parent,
                                                         final DataStoreDescriptor tableData,
                                                         final List<Pattern> hiddenColumnPattern) {
        return tableData.getColumnMetaData()
                        .stream()
                        .map(column -> getColumnAnnotations(parent, column, tableData, hiddenColumnPattern))
                        .flatMap(o -> o.map(Stream::of)
                                       .orElseGet(Stream::empty))
                        .collect(Collectors.toList());
    }

    private Optional<TableAnnotations> getTableAnnotations(final DataStoreDescriptor tableData) {
        // retrieve legacy meta data embedded in table comments
        Optional<TableAnnotations> result = Optional.empty();

        String businessName = businessRules.getExtendedMetadata(tableData);
        String abbrBusinessName = businessRules.getAbbreviatedMetadata(tableData);
        String description = businessRules.getDescription(tableData);
        if (!businessName.trim()
                         .isEmpty() || !abbrBusinessName.trim()
                                                        .isEmpty() || !description.trim()
                                                                                  .isEmpty()) {
            // create Annotation object with default annotations
            TableAnnotations ta = annotationFactory.createTableAnnotations(tableData.getDataModelDescriptor()
                                                                                    .getSchemaName(),
                                                                           tableData.getDataStoreName(),
                                                                           businessName.trim()
                                                                                       .isEmpty() ? null
                                                                                                  : businessName.trim(),
                                                                           abbrBusinessName.trim()
                                                                                           .isEmpty() ? null
                                                                                                      : abbrBusinessName.trim(),
                                                                           description.trim()
                                                                                      .isEmpty() ? null
                                                                                                 : description.trim());
            result = Optional.of(ta);
        }

        // retrieve JSON-based meta data embedded in table comments
        String jsonExpr = businessRules.getMetadata(tableData);
        Optional<TableAnnotations> embedded = Optional.empty();
        if (!jsonExpr.isEmpty()) {
            try {
                // retrieve from annotation in data store
                embedded = this.jsAnnotation.getTableAnnotations(tableData.getDataModelDescriptor()
                                                                          .getSchemaName(),
                                                                 tableData.getDataStoreName(), jsonExpr);
            } catch (MalformedAnnotationException e) {
                String msg = errorWarningMessages.formatMessage(80320, ERROR_MESSAGE_80320, this.getClass(),
                                                                tableData.getDataStoreName(),
                                                                tableData.getDataModelDescriptor()
                                                                         .getSchemaName());
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
                logger.fatal(msg);
                if (throwAnnotationException()) {
                    throw new UnRecoverableException(msg, e);
                }
            }
        }

        if (result.isPresent() && embedded.isPresent()) {
            result.get()
                  .merge(embedded.get());
        } else if (embedded.isPresent()) {
            result = embedded;
        }

        final String keyString = getBaseKey(tableData).toString();
        Key key;
        try {
            key = keyParser.parseKey(keyString);
        } catch (IllegalArgumentException e) {
            key = null;
            String msg = errorWarningMessages.formatMessage(80310, ERROR_MESSAGE_80310, this.getClass(),
                                                            tableData.getDataStoreName());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            logger.fatal(msg);
            if (throwAnnotationException()) {
                throw new UnRecoverableException(msg);
            }
        }
        assert (key != null);

        // retrieve from Annotations.js or Annotations-*.js files
        Optional<TableAnnotations> fileBased = Optional.empty();
        try {
            fileBased = this.jsAnnotation.getTableAnnotations(key);
        } catch (MalformedAnnotationException e) {
            String msg = errorWarningMessages.formatMessage(80300, ERROR_MESSAGE_80300, this.getClass(),
                                                            tableData.getDataStoreName());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            logger.fatal(msg);
            if (throwAnnotationException()) {
                throw new UnRecoverableException(msg, e);
            }
        }

        if (result.isPresent() && fileBased.isPresent()) {
            result.get()
                  .merge(fileBased.get());
        } else if (fileBased.isPresent()) {
            result = fileBased;
        }
        return result;
    }

    @Override
    public Optional<TableAnnotations> getAnnotations(final DataStoreDescriptor tableData,
                                                     final List<Pattern> hiddenColumnPattern) {
        //final List<Pattern> hiddenColumnPattern
        Optional<TableAnnotations> tableAnnotations = getTableAnnotations(tableData);
        if (!tableAnnotations.isPresent()) {
            // create a table annotation in case it was previously not created and
            // at last one column annotation exists
            tableAnnotations = Optional.of(annotationFactory.createTableAnnotations(tableData.getDataModelDescriptor()
                                                                                             .getSchemaName(),
                                                                                    tableData.getDataStoreName(), null,
                                                                                    null, null));
        } else {
            // report violations
            tableAnnotations.get()
                            .isValid();
        }


        List<ColumnAnnotations> cas = getColumnAnnotations(tableAnnotations.get(), tableData, hiddenColumnPattern);
        for (ColumnAnnotations ca : cas) {
            ca.isValid();
            tableAnnotations.get()
                            .addColumnAnnotations(ca);
        }

        Optional<TableAnnotations> result;
        if (!tableAnnotations.get()
                             .isEmpty() || !cas.isEmpty()) {
            result = tableAnnotations;
        } else {
            result = Optional.empty();
        }

        return result;
    }

    @Override
    public List<? extends VariableAnnotations> getVariableAnnotations() {
        return this.jsAnnotation.getVariableAnnotations();
    }
}
