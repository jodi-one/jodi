package one.jodi.core.context.packages;

public class PackageCacheItem implements Comparable<PackageCacheItem> {
    final String name;
    final String packageListItem;
    final int creationOrder;

    public PackageCacheItem(String name, String packageListItem, int creationOrder) {
        this.name = name;
        this.packageListItem = packageListItem;
        this.creationOrder = creationOrder;
    }

    public String getName() {
        return name;
    }

    public int getCreationOrder() {
        return creationOrder;
    }

    public String getPackageListItem() {
        return packageListItem;
    }

    @Override
    public int compareTo(PackageCacheItem that) {
        return this.creationOrder - that.creationOrder;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + creationOrder;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PackageCacheItem other = (PackageCacheItem) obj;
        if (creationOrder != other.creationOrder)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }


}
