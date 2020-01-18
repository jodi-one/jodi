package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.ETLPackageHeader;

public class ETLPackageHeaderImpl implements ETLPackageHeader {

    private final String packageName;
    private final String folderCode;
    private final String projectCode;
    private final String packageListItem;
    private final String comments;

    public ETLPackageHeaderImpl(final String packageName, final String folderCode,
                                final String projectCode,
                                final String packageListItem,
                                final String comments) {
        super();
        this.packageName = packageName;
        this.folderCode = folderCode;
        this.projectCode = projectCode;
        this.packageListItem = packageListItem;
        this.comments = comments;
    }

    @Override
    public String getPackageName() {
        return this.packageName;
    }

    @Override
    public String getFolderCode() {
        return this.folderCode;
    }

    @Override
    public String getProjectCode() {
        return this.projectCode;
    }

    @Override
    public String getPackageListItems() {
        return this.packageListItem;
    }

    @Override
    public String getComments() {
        return comments;
    }

}
