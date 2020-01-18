package one.jodi.base.service.annotation;

import one.jodi.base.error.ErrorWarningMessageJodi;

import java.util.HashMap;
import java.util.Map;

public class JsTestAnnotationImpl extends JsAnnotationImpl
        implements JsTestAnnotation {

    String testFileName = "testFileName";
    private String jsonModel = "{\"Schemas\" : [" +
            "{\"Name\" : \"SCHEMA\"," +
            "\"Tables\" : []}" +
            "]," +
            "\"Variables\" : []}";

    private Map<String, String> fileJsonModelMap = new HashMap<String, String>();

    protected JsTestAnnotationImpl(final String xmlFolderName,
                                   final ErrorWarningMessageJodi errorWarningMessages) {
        super(xmlFolderName, new TestAnnotationFactory(errorWarningMessages),
                errorWarningMessages);
    }

    @Override
    protected Map<String, String> getAnnotations() {
        //ignore file-based access and substitute with injected string
        fileJsonModelMap.put(this.testFileName, this.jsonModel);
        return fileJsonModelMap;
    }

    @Override
    public void setJsModel(final String jsonModel) {
        // inject JSON model into this class
        this.jsonModel = getLowerCaseKeyJson(jsonModel);
        this.fileJsonModelMap.put(this.testFileName, this.jsonModel);
    }


}
