package one.jodi.etl.internalmodel.impl;

import one.jodi.core.config.JodiConstants;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.SetOperatorTypeEnum;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.Transformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of Dataset interface.
 */
public class DatasetImpl implements Dataset {

    private static final Logger logger = LogManager.getLogger(DatasetImpl.class);

    Transformation parent;
    String name;
    List<Source> sources;
    SetOperatorTypeEnum setOperator;

    // Map<String, ExecutionLocationtypeEnum> columnExecutionLocations;

    public DatasetImpl(Transformation parent, String name, SetOperatorTypeEnum setOperator) {
        this.parent = parent;
        this.name = name;
        this.setOperator = setOperator;
        sources = new ArrayList<>();
    }

    public DatasetImpl() {
        sources = new ArrayList<>();
    }

    @Override
    public Transformation getParent() {
        return parent;
    }

    public void setParent(Transformation parent) {
        this.parent = parent;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<Source> getSources() {
        return sources;
    }

    public void addSource(Source source) {
        this.sources.add(source);
    }

    public void clearSources() {
        this.sources.clear();
    }

    @Override
    public SetOperatorTypeEnum getSetOperator() {
        return setOperator;
    }

    public void setSetOperator(SetOperatorTypeEnum setOperator) {
        this.setOperator = setOperator;
    }

    @Override
    public int getDataSetNumber() {
        String number = this.getName()
                            .substring((this.getName()
                                            .lastIndexOf("_") + 1), this.getName()
                                                                        .length());
        // no index but number first number is 1 then 2
        return Integer.parseInt(number) + 1;
    }

    @Override
    public Source getDriverSourceInDataset() {
        return this.getSources()
                   .get(0);
    }

    /*
     * @Override public Map<String, ExecutionLocationtypeEnum>
     * getColumnExecutionLocations() { return columnExecutionLocations; }
     *
     * public void setColumnExecutionLocations(Map<String,
     * ExecutionLocationtypeEnum> columnExecLocs) { columnExecutionLocations =
     * columnExecLocs; }
     */

    @Override
    public List<? extends Source> findJoinedSourcesInDataset() {
        return this.getSources()
                   .subList(1, this.getSources()
                                   .size());
    }


    @Override
    public String translateExpression(String exprText) {
        Map<String, Translation> allAliases = new HashMap<>();
        for (Source s : this.getSources()) {
            String originalSourceAlias = s.getAlias() != null ? s.getAlias() : s.getName();
            String newSourceAlias = s.getComponentName();
            Translation translationSource = new Translation(originalSourceAlias + ".", newSourceAlias + ".");
            allAliases.put(originalSourceAlias + ".", translationSource);
            for (Lookup l : s.getLookups()) {
                String originalLookupAlias = l.getAlias() != null ? l.getAlias() : l.getLookupDataStore();
                String newLookupAlias = l.getComponentName();
                Translation translation = new Translation(originalLookupAlias + ".", newLookupAlias + ".");
                allAliases.put(originalLookupAlias + ".", translation);
            }
        }
        if (exprText == null) {
            exprText = " null ";
        }
        // the reason for this sort is the following;
        // consider alias FCLI. and the alias CLA_FCLI.
        // if we replace all instance of the first with D1FCLI not only the first is affected,
        // but also the second ; CLA_D1FLCI which is wrong,
        // so we sort; the longest key first then all should go well.
        List<Translation> translationsAsList = new ArrayList<>(allAliases.values());
        Collections.sort(translationsAsList, (a, b) -> Integer.compare(b.getKey()
                                                                        .length(), a.getKey()
                                                                                    .length()));
        for (Translation translation : translationsAsList) {
            Pattern p = Pattern.compile(JodiConstants.ALIAS_REGEXP_PREFIX);
            Matcher m = p.matcher(exprText);
            int offset = 0;
            while (m.find()) {
                int start = m.start() + offset;
                int end = m.end() + offset;
                String tableAlias = exprText.substring(start, end);
                if (tableAlias.equals(translation.getKey())) {
                    exprText = exprText.substring(0, start) + translation.getTranslation() +
                            exprText.substring(end, exprText.length());
                    offset += 2;
                    logger.debug("tableAlias: " + tableAlias + " key; " + translation.getKey() + " translation: " +
                                         translation.getTranslation() + " -> " + exprText);
                }
            }
            logger.debug(translation.getKey() + " : " + translation.getTranslation() + " '" + exprText + "'");
        }
        return exprText;
    }

}
