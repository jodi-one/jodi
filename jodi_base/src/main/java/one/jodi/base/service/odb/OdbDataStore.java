/**
 *
 */
package one.jodi.base.service.odb;

class OdbDataStore {

    private final String dataStoreName;
    private final String comments;

    public OdbDataStore(final String dataStoreName, final String comments) {
        super();
        this.dataStoreName = dataStoreName;
        this.comments = comments;
    }

    public String getDataStoreName() {
        return this.dataStoreName;
    }

    public String getDataStoreComments() {
        return this.comments;
    }

}
