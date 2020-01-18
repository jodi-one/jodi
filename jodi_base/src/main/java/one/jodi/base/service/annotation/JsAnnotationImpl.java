package one.jodi.base.service.annotation;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.base.annotations.XmlFolderName;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JsAnnotationImpl implements JsAnnotation {

    private final static Logger logger = LogManager.getLogger(JsAnnotationImpl.class);

    /*
     * JSON Key is assumed to start with a quotation mark followed by
     * at least one characters digit, '_' or whitespace character, followed by
     * a quotation mark and finally a colon.
     */
    private static final String JSON_KEY_PATTERN = "\"([\\w-\\s]{1,})\"\\s*:";

    private final static String ATTRIBUTE_NAME = "Name";

    private final static String ERROR_MESSAGE_80500 =
            "Key '%1$s' encountered to an unexpected type in the JSON object.";

    private final static String ERROR_MESSAGE_80510 =
            "Annotation '%1$s' contains a syntax error.";

    private final static String ERROR_MESSAGE_80520 =
            "Annotation in '%1$s' contains a syntax error. Annotations cannot be " +
                    "retrieved and are ignored. %2$s";

    private final static String ERROR_MESSAGE_80530 =
            "No annotation files were found in directory and subdirectories of '%1$s'.";

    private final static String ERROR_MESSAGE_80531 =
            "The xml directory '%1$s' does not contain annotation files.";

    private final static String ERROR_MESSAGE_80550 =
            "Annotation in '%1$s' does not contain the required 'Name' key in one " +
                    "of its objects. The invalid object is located after the element " +
                    "named '%2$s'. Please correct annotation before proceeding.";

    private final static String ERROR_MESSAGE_80560 =
            "Annotation in '%1$s' does not contain the required value for the 'Name' " +
                    "key in one of its objects. The invalid object is located after the " +
                    "element named '%2$s'. Please correct annotation before proceeding.";

    private final static String ERROR_MESSAGE_80570 =
            "Duplicate column name '%1$s' accessed in table '%2$s' in schema name '%4$s' within file name '%3$s'.";

    private final static String ERROR_MESSAGE_80580 =
            "Duplicate table '%2$s' annotation found in schema '%1$s' within files '%3$s'.";

    private static final String ERROR_MESSAGE_80582 =
            "Failure in getting tables in file name '%1$s' from JSONObject. '%2$s'";

    private static final String ERROR_MESSAGE_80584 =
            "No variables defined from JSONObject in '%1$s'.";

    private static final String ERROR_MESSAGE_80588 =
            "Duplicate variable annotation exist for '%1$s'.";

    private final String xmlFolderName;
    private final AnnotationFactory annotationFactory;
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public JsAnnotationImpl(final @XmlFolderName String xmlFolderName,
                            final AnnotationFactory annotationFactory,
                            final ErrorWarningMessageJodi errorWarningMessages) {
        this.xmlFolderName = xmlFolderName;
        this.annotationFactory = annotationFactory;
        this.errorWarningMessages = errorWarningMessages;
    }

    private List<String> gatherAnnotationFiles(String xmlFolderName2) {
        List<String> aFiles = new ArrayList<String>();
        try {
            aFiles = Files.walk(new File(xmlFolderName2).toPath())
                    .filter(p -> (
                            p.getFileName()
                                    .toString()
                                    .matches("Annotations(-\\w+)?.js")))
                    .map(file -> file.toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            String msg = errorWarningMessages.formatMessage(80531,
                    ERROR_MESSAGE_80531, this.getClass(), xmlFolderName2);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.WARNINGS);
        }

        if (aFiles.isEmpty()) {
            String msg = errorWarningMessages.formatMessage(80531,
                    ERROR_MESSAGE_80531, this.getClass(), this.xmlFolderName);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.WARNINGS);
        }
        return aFiles;
    }

    protected String getLowerCaseKeyJson(final String json) {
        Matcher m = Pattern.compile(JSON_KEY_PATTERN).matcher(json);
        StringBuilder lowerCaseKeyJSON = new StringBuilder();
        int last = 0;
        while (m.find()) {
            lowerCaseKeyJSON.append(json.substring(last, m.start()));
            lowerCaseKeyJSON.append("\"")
                    .append(m.group(1).trim().toLowerCase())
                    .append("\" : ");
            last = m.end();
        }
        lowerCaseKeyJSON.append(json.substring(last));

        return lowerCaseKeyJSON.toString();
    }

    @Cached
    protected Map<String, List<String>> createTableCountMap(
            Map<String, String> fileJsMap) {
        Map<String, List<String>> tableCountMap =
                new HashMap<String, List<String>>();

        for (Entry<String, String> entry : fileJsMap.entrySet()) {
            Map<String, List<String>> map = getTableMap(entry.getKey(), entry.getValue());

            //merge results
            for (Entry<String, List<String>> e : map.entrySet()) {
                tableCountMap.computeIfAbsent(e.getKey(),
                        k -> new ArrayList<String>())
                        .addAll(e.getValue());
            }
        }
        return tableCountMap;
    }

    private void reportIssue(final Entry<String, List<String>> issue) {
        String[] names = issue.getKey().split("\\.");
        assert (names.length == 2);
        String files = issue.getValue()
                .stream()
                .distinct()
                .collect(Collectors.joining(", "));

        String msg = errorWarningMessages.formatMessage(80580,
                ERROR_MESSAGE_80580, this.getClass(), names[0],
                names[1], files);
        errorWarningMessages.addMessage(
                errorWarningMessages.assignSequenceNumber(),
                msg, MESSAGE_TYPE.WARNINGS);
        logger.warn(msg);
    }

    private void reportDuplicateTables(final Map<String, List<String>> countMap) {
        countMap.entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .forEach(e -> reportIssue(e));
    }

    @Override
    public List<? extends VariableAnnotations> getVariableAnnotations()
            throws MalformedAnnotationException {
        Map<String, String> fileContentsMap = getAnnotations();
        return getVariableDefinitions(fileContentsMap);
    }

    @Cached  // Avoids re-reading file many times
    protected Map<String, String> getAnnotations() {
        Map<String, String> resultsMap = new HashMap<String, String>();
        try {
            String result = null;
            for (String file : gatherAnnotationFiles(this.xmlFolderName)) {
                logger.info("Processing JSON file " + file);
                result = new String(Files.readAllBytes(Paths.get(file)),
                        StandardCharsets.UTF_8);
                result = getLowerCaseKeyJson(result);
                resultsMap.put(file, result);
            }
        } catch (IOException e) {
            String msg = errorWarningMessages.formatMessage(80530,
                    ERROR_MESSAGE_80530, this.getClass(),
                    this.xmlFolderName);
            if (!errorWarningMessages.existsWarningMessageWithCode(11023)) {
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.WARNINGS);
                logger.error(msg, e);
            }
        }
        Map<String, List<String>> countMap = createTableCountMap(resultsMap);
        reportDuplicateTables(countMap);
        return resultsMap;
    }

    //
    // translate object from JSON object for a given key
    //
    private Object getJsonValue(final JSONObject jsonObject, final String memberName) {
        assert (jsonObject != null);
        Object result = null;
        try {
            JSONObject json = jsonObject;
            result = json.get(memberName);
            if (JSONObject.NULL == result) {
                result = null;
            }
        } catch (JSONException e) {
            if (!e.getMessage().contains("not found")) {
                // represents an exception that may indicate that the JSON descriptor
                // is malformed.
                String msg = errorWarningMessages.formatMessage(80510,
                        ERROR_MESSAGE_80510, this.getClass(),
                        jsonObject.toString());
                logger.error(msg, e);
                throw new MalformedAnnotationException(msg, e);
            }
        }
        return result;
    }

    private JSONObject getJsByName(final String memberName,
                                   final JSONArray jsObjects,
                                   final String keyFileName) {
        assert (jsObjects != null);

        JSONObject result = null;
        String lastName = "";
        try {
            for (int i = 0; i < jsObjects.length(); i++) {
                JSONObject element = jsObjects.getJSONObject(i);
                Object obj = element.get(ATTRIBUTE_NAME.toLowerCase());
                if (obj == null || (!(obj instanceof String)) ||
                        ((String) obj).trim().isEmpty()) {
                    String msg = errorWarningMessages.formatMessage(80560,
                            ERROR_MESSAGE_80560, this.getClass(),
                            this.xmlFolderName + "/" + keyFileName,
                            lastName);
                    errorWarningMessages.addMessage(
                            errorWarningMessages.assignSequenceNumber(),
                            msg, MESSAGE_TYPE.ERRORS);
                    logger.error(msg);
                    throw new MalformedAnnotationException(msg);
                } else if (((String) obj).equalsIgnoreCase(memberName)) {
                    result = element;
                    break;
                } else {
                    lastName = (String) obj;
                }
            }
        } catch (JSONException e) {
            String msg = errorWarningMessages.formatMessage(80550,
                    ERROR_MESSAGE_80550, this.getClass(),
                    this.xmlFolderName + "/" + keyFileName,
                    lastName);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg, e);
            throw new MalformedAnnotationException(msg, e);
        }
        return result;
    }

    //
    // Implement simple JSPath to get table or column attribute
    //

    @Cached // avoids parsing String repeatedly
    protected JSONObject getJsonObject(String jsString, String keyFileName) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsString);
        } catch (JSONException e) {
            String msg = errorWarningMessages.formatMessage(80520,
                    ERROR_MESSAGE_80520, this.getClass(),
                    this.xmlFolderName + "/" + keyFileName,
                    e.getMessage());
            if (!errorWarningMessages.existsErrorMessageWithCode(11022)) {
                // only add message once as a schema failure in the Annotation.js
                // file will result in hundreds or so identical messages.
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg, e);
            }
            throw new MalformedAnnotationException(msg, e);
        }
        return jsonObject;
    }

    private JSONArray findArrayWithKeyName(final JSONObject root, final String key,
                                           final Key fullKey) {
        Object array = getJsonValue(root, key.toLowerCase());
        if (array == null) {
            logger.warn("JSON text defines a null array for member " + key);
            return null;
        } else if (!(array instanceof JSONArray)) {
            String msg = errorWarningMessages.formatMessage(80500,
                    ERROR_MESSAGE_80500, this.getClass(),
                    fullKey.toString());
            logger.error(msg);
            throw new MalformedAnnotationException(msg);
        }
        return (JSONArray) array;
    }

    private JSONObject getJsonObjectForBaseKey(final Key key,
                                               final String jsString,
                                               final String keyFileName) {

        if (jsString == null || jsString.trim().isEmpty()) {
            return null;
        }

        JSONObject jsonObject = getJsonObject(jsString, keyFileName);
        for (NameSpaceComponent ns : key.getNameSpace()) {
            String arrayKey = ns.getType().getComponentName();

            // find array value associated with array keyword
            Object array = findArrayWithKeyName(jsonObject, arrayKey.toLowerCase(),
                    key);
            if (array == null) {
                logger.debug("JSON object for key " + key.toString() +
                        " was not found. Expected array with key " + arrayKey +
                        " was not found.");
                return null;
            }

            String nameKey = ns.getName();
            // find object in array that has the expected name
            jsonObject = getJsByName(nameKey, (JSONArray) array, keyFileName);
            if (jsonObject == null) {
                logger.debug("JSON element with \"" + ATTRIBUTE_NAME + "\" " +
                        nameKey + " was not found in array.");
                return null;
            }
        }

        return jsonObject;
    }

    private Optional<FileContentPair> getJsString(final Key key,
                                                  final Map<String, String> jsStringMap) {
        Optional<FileContentPair> fileNameJsContents = Optional.empty();

        // create map of schema and table string, and add file name for each occurrence
        Map<String, List<String>> tableCountMap = createTableCountMap(jsStringMap);

        String tableKey = key.getNameSpace().get(0).getName() + "." +
                key.getNameSpace().get(1).getName();
        List<String> count = tableCountMap.get(tableKey);
        if (count != null) {
            String fileName = jsStringMap.get(count.get(0));
            fileNameJsContents = Optional.of(new FileContentPair(count.get(0), fileName));
        }

        //TODO remove this after other refactoring is done
        if (!fileNameJsContents.isPresent()) {
            Map.Entry<String, String> entry = jsStringMap.entrySet().iterator().next();
            fileNameJsContents =
                    Optional.of(new FileContentPair(entry.getKey(), entry.getValue()));
        }
        return fileNameJsContents;
    }

    private Map<String, List<String>> getTableMap(final String filename,
                                                  final String filecontents) {
        Map<String, List<String>> tableFileMapping = new HashMap<>();
        JSONObject jsonObject;
        try {
            jsonObject = getJsonObject(filecontents, filename);
            JSONArray schemas = jsonObject.getJSONArray(KeyParser.NS_SCHEMA.toLowerCase());
            for (int i = 0; i < schemas.length(); i++) {
                JSONObject schema = (JSONObject) schemas.get(i);
                String schemaName = schema.getString(ATTRIBUTE_NAME.toLowerCase());
                JSONArray tables = schema.getJSONArray(KeyParser.NS_TABLE.toLowerCase());
                for (int j = 0; j < tables.length(); j++) {
                    JSONObject table = (JSONObject) tables.get(j);
                    String tableName = table.getString(ATTRIBUTE_NAME.toLowerCase());
                    String key = schemaName + "." + tableName;
                    tableFileMapping.computeIfAbsent(key, k -> new ArrayList<String>())
                            .add(filename);
                }
            }
        } catch (JSONException | MalformedAnnotationException e) {
            String msg = errorWarningMessages.formatMessage(80582,
                    ERROR_MESSAGE_80582, this.getClass(),
                    filename, e.getMessage());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg, e);
        }
        return tableFileMapping;
    }

    private Object convertArray(final Object object) {
        Object converted = object;
        if (object instanceof JSONArray) {
            JSONArray json = (JSONArray) object;
            converted = collectArrayAnnotations(json, null);
        }
        return converted;
    }

    //
    // New Development
    //

    private boolean arrayExpected(final Object json, final boolean excludeArrays) {
        // Findbugs issue BC excluded
        return !(json instanceof JSONArray) ||
                json instanceof JSONArray && !excludeArrays;
    }

    private String toLower(final String exludedName) {
        return exludedName == null ? "" : exludedName.toLowerCase();
    }

    private Map<String, Object> collectAnnotations(final JSONObject json,
                                                   final String exludedName,
                                                   final boolean excludeArrays) {
        final Map<String, Object> annotations;
        Set<String> keys = json.keySet();
        annotations =
                keys.stream()
                        .filter(key -> !(key.equals(toLower(exludedName))) &&
                                !JSONObject.NULL.equals(json.opt(key)))
//              .peek(k -> checkTypeViolation(key, jsonObject.opt(key)))
                        .filter(key -> !(json.opt(key) instanceof JSONObject) &&
                                arrayExpected(json.opt(key), excludeArrays))
                        .collect(Collectors.toMap(String::toLowerCase,
                                v -> convertArray(json.opt(v))));
        return annotations;
    }

    private List<Object> collectArrayAnnotations(final JSONArray json,
                                                 final String exludedName) {
        List<Object> array = new ArrayList<>();
        for (int index = 0; index < json.length(); index++) {
            if (json.isNull(index)) {
                array.add(null);
            } else if (json.opt(index) instanceof Number ||
                    json.opt(index) instanceof String ||
                    json.opt(index) instanceof Boolean) {
                array.add(json.get(index));
            } else if (!json.isNull(index)) {
                array.add(collectAnnotations(json.getJSONObject(index), exludedName, true));
            }
        }
        return array;
    }

    private Map<String, List<Object>> collectAnnotationArrays(final JSONObject json,
                                                              final String exludedName) {
        final Map<String, List<Object>> annotations;
        Set<String> keys = json.keySet();
        annotations =
                keys.stream()
                        .filter(key -> !(key.equals(toLower(exludedName))) &&
                                !JSONObject.NULL.equals(json.opt(key)))
                        .filter(key -> (json.opt(key) instanceof JSONArray))
                        .collect(Collectors.toMap(String::toLowerCase,
                                key -> collectArrayAnnotations(
                                        (JSONArray) json.opt(key),
                                        exludedName)));
        return annotations;
    }

    // process 'flat' json object with annotations
    private Map<String, Object> getAnnotations(final String jsonExpr,
                                               final boolean excludeArrays)
            throws MalformedAnnotationException {
        Map<String, Object> annotations;
        try {
            JSONObject json = new JSONObject(jsonExpr);
            // collect annotations
            annotations = collectAnnotations(json, null, excludeArrays);
        } catch (JSONException e) {
            // represents an exception that may indicate that the JSON descriptor
            // is malformed.
            String msg = errorWarningMessages.formatMessage(80510,
                    ERROR_MESSAGE_80510, this.getClass(),
                    jsonExpr);
            logger.error(msg, e);
            throw new MalformedAnnotationException(msg, e);
        }
        return annotations;
    }

    // process array of json object with annotations
    private Map<String, List<Object>> getArrayAnnotations(final String jsonExpr)
            throws MalformedAnnotationException {
        Map<String, List<Object>> annotations;
        try {
            JSONObject json = new JSONObject(jsonExpr);
            // collect annotations
            annotations = collectAnnotationArrays(json, null);

        } catch (JSONException e) {
            // represents an exception that may indicate that the JSON descriptor
            // is malformed.
            String msg = errorWarningMessages.formatMessage(80510,
                    ERROR_MESSAGE_80510, this.getClass(),
                    jsonExpr);
            logger.error(msg, e);
            throw new MalformedAnnotationException(msg, e);
        }
        return annotations;
    }

    private Optional<ColumnAnnotations> getColumnAnnotations(
            final TableAnnotations parent,
            final String columnName,
            final Map<String, Object> annotations) {
        Optional<ColumnAnnotations> optionalAnnotations;
        if (annotations.isEmpty()) {
            optionalAnnotations = Optional.empty();
        } else {
            ColumnAnnotations ta = annotationFactory.createColumnAnnotations(parent,
                    columnName);
            ta.initializeAnnotations(annotations);
            optionalAnnotations = ta.isEmpty() ? Optional.empty() : Optional.of(ta);
        }
        return optionalAnnotations;
    }

    @Override
    public Optional<ColumnAnnotations> getColumnAnnotations(
            final TableAnnotations parent,
            final String columnName,
            final String jsonExpr)
            throws MalformedAnnotationException {
        Map<String, Object> annotations = getAnnotations(jsonExpr, false);
        Optional<ColumnAnnotations> optionalAnnotations =
                getColumnAnnotations(parent, columnName, annotations);
        return optionalAnnotations;
    }

    @Override
    public Optional<TableAnnotations> getTableAnnotations(final String schemaName,
                                                          final String tableName,
                                                          final String jsonExpr)
            throws MalformedAnnotationException {
        Map<String, Object> annotations = getAnnotations(jsonExpr, true);
        Optional<TableAnnotations> optionalAnnotations;
        if (annotations.isEmpty()) {
            optionalAnnotations = Optional.empty();
        } else {
            TableAnnotations ta = annotationFactory.createTableAnnotations(schemaName,
                    tableName);
            ta.initializeAnnotations(annotations);
            optionalAnnotations = ta.isEmpty() ? Optional.empty() : Optional.of(ta);
        }

        // add processing of calculated column definitions
        Map<String, List<Object>> arrayAnnotations = getArrayAnnotations(jsonExpr);
        List<Map<String, Object>> calculatedColumns = getColumnsArray(arrayAnnotations);
        if (!optionalAnnotations.isPresent() && !calculatedColumns.isEmpty()) {
            TableAnnotations ta = annotationFactory.createTableAnnotations(schemaName,
                    tableName);
            for (Map<String, Object> calcColumn : calculatedColumns) {
                String name = (String) calcColumn.get("name");
                if (name == null) continue;
                calcColumn.remove("name");
                Optional<ColumnAnnotations> ca =
                        getColumnAnnotations(ta, name, calcColumn);
                if (ca.isPresent()) {
                    ta.addColumnAnnotations(ca.get());
                }
            }
            optionalAnnotations = ta.getColumnAnnotations().isEmpty() ? Optional.empty()
                    : Optional.of(ta);
        }
        return optionalAnnotations;
    }

    private List<VariableAnnotations> getVariableDefinitions(
            Map<String, String> resultsMap)
            throws MalformedAnnotationException {
        Set<String> processedVariableNames = new HashSet<>();
        List<VariableAnnotations> variableAnnotations = new ArrayList<VariableAnnotations>();
        JSONObject jsonObject;
        for (Entry<String, String> entry : resultsMap.entrySet()) {
            try {
                jsonObject = getJsonObject(entry.getValue(), entry.getKey());
                JSONArray variables = jsonObject.getJSONArray(KeyParser.NS_VARIABLES.toLowerCase());

                // get the variables
                for (int i = 0; i < variables.length(); i++) {
                    JSONObject variable = (JSONObject) variables.get(i);
                    Map<String, Object> annotations = collectAnnotations(variable, null, false);
                    String name = variable.getString(ATTRIBUTE_NAME.toLowerCase());

                    // determine if duplicate name exists and skip if it does
                    if (processedVariableNames.contains(name.toLowerCase())) {
                        String msg = errorWarningMessages.formatMessage(80588,
                                ERROR_MESSAGE_80588, this.getClass(),
                                name);
                        errorWarningMessages.addMessage(
                                errorWarningMessages.assignSequenceNumber(),
                                msg, MESSAGE_TYPE.ERRORS);
                        logger.error(msg);
                        continue;
                    } else {
                        // add lower case name to set
                        processedVariableNames.add(name.toLowerCase());
                    }

                    VariableAnnotations vAnnotation =
                            annotationFactory.createVariableAnnotations(name);
                    vAnnotation.initializeAnnotations(annotations);
                    if (vAnnotation.isValid()) {
                        variableAnnotations.add(vAnnotation);
                    }

                }
            } catch (JSONException e) {
                String msg = errorWarningMessages.formatMessage(80584,
                        ERROR_MESSAGE_80584, this.getClass(), entry.getKey());
                logger.debug(msg);
            }
        }
        return variableAnnotations;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getColumnsArray(
            final Map<String, List<Object>> arrayAnnotations) {
        List<Map<String, Object>> result;
        List<Object> columns = arrayAnnotations.get(KeyParser.NS_COLUMN.toLowerCase());
        if (columns == null) {
            result = Collections.emptyList();
        } else {
            result = columns.stream()
                    .map(c -> (Map<String, Object>) c)
                    .collect(Collectors.toList());
        }
        return result;
    }

    // Note: keyword is in lower case!
    private Map<String, Map<String, Object>> getColumnAnnotations(
            final JSONObject json,
            final Key fullKey,
            final String keyFileName) {
        Map<String, Map<String, Object>> annotationMap = new HashMap<>();

        JSONArray array = findArrayWithKeyName(json, KeyParser.NS_COLUMN, fullKey);

        if (array == null) return annotationMap; // return empty map

        String lastName = "";
        for (int i = 0; i < array.length(); i++) {
            Object jsObject = array.opt(i);
            if (JSONObject.NULL == jsObject || !(jsObject instanceof JSONObject)) {
                String msg = errorWarningMessages.formatMessage(80560,
                        ERROR_MESSAGE_80560, this.getClass(),
                        this.xmlFolderName + "/" + keyFileName,
                        lastName);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                continue;
            }

            Map<String, Object> annotations = collectAnnotations(
                    (JSONObject) jsObject, null, false);

            Object cObj = annotations.get(ATTRIBUTE_NAME.toLowerCase());
            if (JSONObject.NULL == cObj || !(cObj instanceof String) ||
                    ((String) cObj).trim().isEmpty()) {
                String msg = errorWarningMessages.formatMessage(80560,
                        ERROR_MESSAGE_80560, this.getClass(),
                        this.xmlFolderName + "/" + keyFileName,
                        lastName);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                throw new MalformedAnnotationException(msg);
            }
            String columnName = (String) cObj;
            annotations.remove(ATTRIBUTE_NAME.toLowerCase());

            lastName = columnName;

            if (annotations.isEmpty()) continue;

            if (annotationMap.get(columnName) != null) {
                String msg = errorWarningMessages.formatMessage(80570,
                        ERROR_MESSAGE_80570, this.getClass(),
                        columnName,
                        fullKey.getNameSpace().get(1).getName(),
                        keyFileName,
                        fullKey.getNameSpace().get(0).getName());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(),
                        msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
                throw new MalformedAnnotationException(msg);
            }

            annotationMap.put(columnName, annotations);
        }

        return annotationMap;
    }

    @Override
    public Optional<TableAnnotations> getTableAnnotations(final Key baseKey)
            throws MalformedAnnotationException {
        assert (baseKey.getNameSpace().size() == 2);

        Map<String, String> jsStringMap = getAnnotations();

        if (jsStringMap.isEmpty()) {
            String msg = errorWarningMessages.formatMessage(80531, ERROR_MESSAGE_80531,
                    this.getClass(),
                    this.xmlFolderName);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.WARNINGS);
            return Optional.empty();
        }

        Optional<FileContentPair> fcp = getJsString(baseKey, jsStringMap);
        if (!fcp.isPresent()) {
            return Optional.empty();
        }

        String keyFileName = fcp.get().getFileName();
        String jsString = fcp.get().getFileContents();

        JSONObject json = getJsonObjectForBaseKey(baseKey, jsString, keyFileName);
        if (json == null) return Optional.empty();

        final Map<String, Object> annotations;
        // collect table annotations
        annotations = collectAnnotations(json, KeyParser.NS_COLUMN, false);
        annotations.remove(ATTRIBUTE_NAME.toLowerCase());

        // construct Table Annotation
        String schemaName = baseKey.getNameSpace().get(0).getName();
        String tableName = baseKey.getNameSpace().get(1).getName();
        TableAnnotations ta = annotationFactory.createTableAnnotations(schemaName,
                tableName);
        ta.initializeAnnotations(annotations);

        Map<String, Map<String, Object>> columnAnnotations =
                getColumnAnnotations(json, baseKey, keyFileName);

        for (Entry<String, Map<String, Object>> e : columnAnnotations.entrySet()) {
            ta.addColumnAnnotations(e.getKey(), e.getValue());
        }

        return Optional.of(ta);
    }

    static class FileContentPair {
        private final String fileName;
        private final String fileContents;

        private FileContentPair(final String fileName,
                                final String fileContents) {
            super();
            this.fileName = fileName;
            this.fileContents = fileContents;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFileContents() {
            return fileContents;
        }
    }


}
