package one.jodi.base.service.annotation;

import one.jodi.base.config.BaseConfigurations;
import one.jodi.base.config.BaseConfigurationsHelper;
import one.jodi.base.error.ErrorWarningMessageJodi;

public class BaseAnnotationServiceHelper {

    public final static JsTestAnnotation createJsTestAnnotation(
            final ErrorWarningMessageJodi errorWarningMessages) {
        return new JsTestAnnotationImpl("ignored", errorWarningMessages);
    }


    public final static AnnotationService createAnnotationServiceInstance(
            final ErrorWarningMessageJodi errorWarningMessages) {
        errorWarningMessages.clear();
        BaseConfigurations b = BaseConfigurationsHelper.getTestBaseConfigurations();
        TableBusinessRules businessRules = new TableBusinessRulesImpl(b);

        AnnotationFactory annotationFactory =
                new TestAnnotationFactory(errorWarningMessages);
        JsAnnotation jsAnnotation = new JsAnnotationImpl("pathToTheFolder",
                annotationFactory,
                errorWarningMessages);
        KeyParser keyParser = new KeyParserImpl(errorWarningMessages);
        return new AnnotationServiceDefaultImpl(businessRules, jsAnnotation,
                keyParser, annotationFactory, b,
                errorWarningMessages);
    }

}
