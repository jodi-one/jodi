package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.*;
import one.jodi.model.extensions.SourceExtension;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of Source interface.
 *
 */
public class SourceImpl implements Source {

    Dataset parent;
    String model;
    String name;
    String alias;
    String filter;
    ExecutionLocationtypeEnum filterExecutionLocation;
    String join;
    JoinTypeEnum joinType;
    ExecutionLocationtypeEnum joinExecutionLocation;
    LinkedList<Lookup> lookups;
    LinkedList<Flow> flows = new LinkedList<>();
    boolean isSubSelect;
    boolean isTemporary;
    KmType lkm;
    SourceExtension extension;
    boolean journalized;
    //String temporaryDataStore;
    List<String> subscribers = new ArrayList<>();

    public SourceImpl(Dataset parent,
                      String model,
                      String name,
                      String alias,
                      String filter,
                      ExecutionLocationtypeEnum filterExecutionLocation,
                      String join,
                      JoinTypeEnum joinType,
                      ExecutionLocationtypeEnum joinExecutionLocation,
                      boolean isSubSelect,
                      boolean isTemporary,
                      boolean journalized/*,
			String temporaryDataStore*/) {
        super();
        this.parent = parent;
        this.model = model;
        this.name = name;
        this.alias = alias;
        this.filter = filter;
        this.filterExecutionLocation = filterExecutionLocation;
        this.join = join;
        this.joinType = joinType;
        this.joinExecutionLocation = joinExecutionLocation;
        this.isSubSelect = isSubSelect;
        this.isTemporary = isTemporary;
        this.lookups = new LinkedList<>();
        this.journalized = journalized;
        this.flows = new LinkedList<>();
        //this.temporaryDataStore = temporaryDataStore;
    }


    public SourceImpl() {
        lookups = new LinkedList<>();
    }


    @Override
    public Dataset getParent() {
        return parent;
    }

    public void setParent(Dataset parent) {
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
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String getFilter() {
        return filter != null ? filter : "";
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    @Override
    public ExecutionLocationtypeEnum getFilterExecutionLocation() {
        return filterExecutionLocation;
    }

    public void setFilterExecutionLocation(
            ExecutionLocationtypeEnum filterExecutionLocation) {
        this.filterExecutionLocation = filterExecutionLocation;
    }

    @Override
    public String getJoin() {
        return join;
    }

    public void setJoin(String join) {
        this.join = join;
    }

    @Override
    public JoinTypeEnum getJoinType() {
        return joinType;
    }

    public void setJoinType(JoinTypeEnum joinType) {
        this.joinType = joinType;
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
    public List<Lookup> getLookups() {
        return lookups;
    }

    public void addLookup(Lookup lookup) {
        lookups.add(0, lookup);
    }

    public void clearLookups() {
        this.lookups.clear();
    }

    @Override
    public boolean isSubSelect() {
        return isSubSelect;
    }

    public void setSubSelect(boolean isSubSelect) {
        this.isSubSelect = isSubSelect;
    }

    @Override
    public boolean isTemporary() {
        return isTemporary;
    }

    public void setTemporary(boolean isTemporary) {
        this.isTemporary = isTemporary;
    }


    @Override
    public KmType getLkm() {
        return lkm;
    }


    public void setLkm(KmType lkm) {
        this.lkm = lkm;
    }

    @Override
    public SourceExtension getExtension() {
        return extension;
    }

    public void setExtension(SourceExtension extension) {
        this.extension = extension;
    }

    @Override
    public boolean isJournalized() {
        return journalized;
    }

    public void setJournalizedType(boolean journalized) {
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
    public Source findJoinedSource() {
        List<Source> allSources = this.getParent().getSources();
        String secondAlias = getComponentName();
        String secondJoin = this.getJoin();
        Pattern p = Pattern
                .compile("([A-Za-z0-9\\_$]{1,})(.){1,1}([A-Za-z0-9\\_$]{1,})");
        Matcher m = p.matcher(secondJoin);
        Set<String> aliases = new HashSet<>();
        while (m.find()) {
            String aliasAndColumn = secondJoin.substring(m.start(), m.end());
            if (aliasAndColumn.indexOf(".") > 0) {
                String alias = aliasAndColumn.substring(0,
                        aliasAndColumn.indexOf("."));
                if (alias.equals(secondAlias)) {
                    aliases.add(alias);
                }
            }
        }
        Source found = null;
        for (Source s : allSources) {
            String alias = getComponentName();
            if (aliases.contains(alias)) {
                found = s;
            }
        }
        assert (found != null);
        return found;
    }

    @Override
    public String getComponentName() {
        int dataSetNumber = this.getParent().getDataSetNumber();
        String alias = this.getAlias() != null ? this.getAlias() : this
                .getName();
        return ComponentPrefixType.DATASET.getAbbreviation() + dataSetNumber
                + alias;
    }


    @Override
    public List<Flow> getFlows() {
        return flows;
    }


    public boolean add(Flow e) {
        return flows.add(e);
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
}
