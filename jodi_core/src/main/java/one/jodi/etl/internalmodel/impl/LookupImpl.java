package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Lookup interface.
 */
public class LookupImpl implements Lookup {

    Source parent;
    String model;
    String lookupDataStore;
    String alias;
    String join;
    LookupTypeEnum lookupType;
    ExecutionLocationtypeEnum joinExecutionLocation;
    KmType lkm;
    boolean temporary;
    boolean subSelect;
    boolean journalized;
    //String temporaryDataStore;
    List<String> subscribers = new ArrayList<>();
    LinkedHashMap<String, String> defaultColumns = new LinkedHashMap<String, String>();

    public LookupImpl(Source parent,
                      String model,
                      String lookupDataStore,
                      String alias,
                      String join,
                      LookupTypeEnum lookupType,
                      ExecutionLocationtypeEnum joinExecutionLocation,
                      KmType lkm,
                      boolean temporary,
                      boolean subSelect,
                      boolean journalized/*,
			String temporaryDataStore*/) {
        this.parent = parent;
        this.model = model;
        this.lookupDataStore = lookupDataStore;
        this.alias = alias;
        this.join = join;
        this.lookupType = lookupType;
        this.joinExecutionLocation = joinExecutionLocation;
        this.lkm = lkm;
        this.temporary = temporary;
        this.subSelect = subSelect;
        this.journalized = journalized;
        //this.temporaryDataStore = temporaryDataStore;
    }

    public LookupImpl() {

    }

    @Override
    public Source getParent() {
        return parent;
    }

    public void setParent(Source parent) {
        this.parent = parent;
    }

    @Override
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String getLookupDataStore() {
        return lookupDataStore;
    }

    public void setLookupDatastore(String lookupDataStore) {
        this.lookupDataStore = lookupDataStore;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String getJoin() {
        return join;
    }

    public void setJoin(String join) {
        this.join = join;
    }

    @Override
    public LookupTypeEnum getLookupType() {
        return lookupType;
    }


    public void setLookupType(LookupTypeEnum lookupType) {
        this.lookupType = lookupType;
    }

    @Override
    public ExecutionLocationtypeEnum getJoinExecutionLocation() {
        return joinExecutionLocation;
    }

    public void setJoinExecutionLocation(
            ExecutionLocationtypeEnum joinExecutionLocation) {
        this.joinExecutionLocation = joinExecutionLocation;
    }

    @Override
    public KmType getLkm() {
        return lkm;
    }

    public void setLkm(KmType lkm) {
        this.lkm = lkm;
    }

    @Override
    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }

    @Override
    public boolean isSubSelect() {
        return subSelect;
    }

    public void setSubSelect(boolean subSelect) {
        this.subSelect = subSelect;
    }

    @Override
    public boolean isJournalized() {
        return journalized;
    }

    public void setJournalized(boolean journalized) {
        this.journalized = journalized;
    }

    /*
     * @Override public String getTemporaryDataStore() { return
     * temporaryDataStore; }
     *
     * public void setTemporaryDataStore(String temporaryDataStore) {
     * this.temporaryDataStore = temporaryDataStore; }
     */

    @Override
    public String getComponentName() {
        int dataSetNumber = this.getParent().getParent().getDataSetNumber();
        String alias = this.getAlias() != null ? this.getAlias() :
                this.getLookupDataStore();
        return ComponentPrefixType.DATASET.getAbbreviation() +
                dataSetNumber + alias;
    }

    //TODO MOVE into Dataset
    @Override
    public int getNumberOfLookupsInDataset() {
        int maxNumber = 0;
        for (Source s : this.getParent().getParent().getSources()) {
            if (s.getLookups() != null) {
                maxNumber += s.getLookups().size();
            }
        }
        return maxNumber;
    }

    @Override
    public List<String> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<String> subscribers) {
        this.subscribers = subscribers;
    }

    @Override
    public String getJournalizedFilters() {
        StringBuilder filter = new StringBuilder();
        for (String subscriber : this.subscribers) {
            filter.append("JRN_SUBSCRIBER='" + subscriber + "' or ");
        }
        if (this.subscribers.size() > 0) {
            return filter.toString().substring(0, filter.toString().length() - 4);
        } else {
            return "JRN_SUBSCRIBER = 'SUNOPSIS'";
        }
    }

    @Override
    public Map<String, String> getDefaultRowColumns() {
        return defaultColumns;
    }
}
