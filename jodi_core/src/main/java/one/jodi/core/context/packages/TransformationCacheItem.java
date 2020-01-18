package one.jodi.core.context.packages;

public class TransformationCacheItem implements Comparable<TransformationCacheItem> {
    final String name;
    final Integer packageSequence;
    final String packageList;
    final String folderName;
    final boolean asynchronous;

    public TransformationCacheItem(String name, Integer packageSequence, String packageListItem, String folderName, boolean asynchronous) {
        this.name = name;
        this.packageSequence = packageSequence;
        this.packageList = packageListItem;
        this.folderName = folderName;
        this.asynchronous = asynchronous;
    }

    public String getName() {
        return this.name;
    }

    public Integer getPackageSequence() {
        return this.packageSequence;
    }

    public String getPackageList() {
        return packageList;
    }

    @Override
    public int compareTo(TransformationCacheItem that) {
        return this.packageSequence - that.packageSequence;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((packageSequence == null) ? 0 : packageSequence.hashCode());
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
        TransformationCacheItem other = (TransformationCacheItem) obj;

        if (packageSequence == null) {
            if (other.packageSequence != null)
                return false;
        } else if (!packageSequence.equals(other.packageSequence))
            return false;
        return true;
    }

    public String getFolderName() {
        return folderName;
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }
}
