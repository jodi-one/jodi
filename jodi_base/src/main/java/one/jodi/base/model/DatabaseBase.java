package one.jodi.base.model;

public class DatabaseBase {

    private final String name;
    private final String dataBaseServiceName;
    private final int dataBaseServicePort;
    private final String physicalServerName;

    public DatabaseBase(final String name, final String dataBaseServiceName,
                        final int dataBaseServicePort,
                        final String physicalServerName) {
        this.name = name;
        this.physicalServerName = physicalServerName;
        this.dataBaseServiceName = dataBaseServiceName;
        this.dataBaseServicePort = dataBaseServicePort;
    }

    public String getName() {
        return this.name;
    }

    public String getDataBaseServiceName() {
        return dataBaseServiceName;
    }

    public int getDataBaseServicePort() {
        return dataBaseServicePort;
    }

    public String getPhysicalServerName() {
        return physicalServerName;
    }

}
