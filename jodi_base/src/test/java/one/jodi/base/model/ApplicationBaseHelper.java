package one.jodi.base.model;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.service.annotation.AnnotationService;
import one.jodi.base.service.annotation.BaseAnnotationServiceHelper;
import one.jodi.base.service.metadata.DataModelDescriptor;

import java.util.Map;

public class ApplicationBaseHelper {

    public static SchemaBase createMockSchema(
            final ErrorWarningMessageJodi errorWarningMessages) {
        final AnnotationService annotationService =
                BaseAnnotationServiceHelper
                        .createAnnotationServiceInstance(errorWarningMessages);

        ApplicationBase application =
                new ApplicationBase(annotationService, errorWarningMessages);

        DataModelDescriptor model = new DataModelDescriptor() {
            @Override
            public String getModelCode() {
                return "SCHEMA";
            }

            @Override
            public Map<String, Object> getModelFlexfields() {
                return null;
            }

            @Override
            public String getDataServerName() {
                return "name";
            }

            @Override
            public String getPhysicalDataServerName() {
                return "dbserver";
            }

            @Override
            public String getDataBaseServiceName() {
                return "Srvs_01";
            }

            @Override
            public int getDataBaseServicePort() {
                return 1521;
            }

            @Override
            public String getDataServerTechnology() {
                return "Oracle";
            }

            @Override
            public String getSchemaName() {
                return "SCHEMA";
            }
        };
        return application.addSchema(model, "valueof(puser)");
    }
}
