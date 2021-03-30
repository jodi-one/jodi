package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.KmType;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.model.extensions.MappingsExtension;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of Mappings interface.
 */
public class MappingsImpl implements Mappings {

    Transformation parent;
    boolean distinct;
    String model;
    String stagingModel;
    String targetDataStore;
    List<Targetcolumn> targetcolumns;
    KmType ikm;
    KmType ckm;
    MappingsExtension extension;
    //String temporaryDataStore;


    public MappingsImpl(Transformation parent,
                        boolean distinct,
                        String model,
                        String targetDataStore,
                        KmType ikm,
                        KmType ckm/*,
			String temporaryDataStore*/) {
        this.parent = parent;
        this.distinct = distinct;
        this.model = model;
        this.targetDataStore = targetDataStore;
        this.ikm = ikm;
        this.ckm = ckm;
        this.targetcolumns = new LinkedList<>();
        //this.temporaryDataStore = temporaryDataStore;
    }

    public MappingsImpl() {
        targetcolumns = new LinkedList<>();
    }

    @Override
    public Transformation getParent() {
        return parent;
    }

    public void setParent(Transformation parent) {
        this.parent = parent;
    }

    @Override
    public boolean isDistinct() {
        return distinct;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String getStagingModel() {
        return stagingModel;
    }

    public void setStagingModel(String stagingModel) {
        this.stagingModel = stagingModel;
    }


    @Override
    public String getTargetDataStore() {
        return targetDataStore;
    }

    public void setTargetDataStore(String targetDataStore) {
        this.targetDataStore = targetDataStore;
    }

    @Override
    public List<Targetcolumn> getTargetColumns() {
        // TODO we expose the list itself, opening it for changes by calling logic - is that what we want?
        targetcolumns.sort(Comparator.comparingInt(Targetcolumn::getPosition));
        return targetcolumns;
    }

    public void addTargetcolumns(Targetcolumn targetcolumn) {
        this.targetcolumns.add(targetcolumn);
    }

    public void clearTargetcolumns() {
        this.targetcolumns.clear();
    }

    public void removeAllTargetcolumns(List<Targetcolumn> removes) {
        targetcolumns.removeAll(removes);
    }

    @Override
    public KmType getIkm() {
        return ikm;
    }

    public void setIkm(KmType ikm) {
        this.ikm = ikm;
    }

    @Override
    public KmType getCkm() {
        return ckm;
    }

    public void setCkm(KmType ckm) {
        this.ckm = ckm;
    }

    @Override
    public MappingsExtension getExtension() {
        return extension;
    }

    public void setExtension(MappingsExtension extension) {
        this.extension = extension;
    }

    @Override
    public boolean hasUpdateKeys() {
        return this.targetcolumns.stream().anyMatch(Targetcolumn::isUpdateKey);
    }

    @Override
    public boolean isAggregateTransformation(int dataSetNumber) {
        return this.targetcolumns.stream().anyMatch(tc -> tc.isAggregateColumn(dataSetNumber));
    }
}