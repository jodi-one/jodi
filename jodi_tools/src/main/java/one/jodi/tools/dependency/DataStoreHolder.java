package one.jodi.tools.dependency;


public class DataStoreHolder {
    final String name;
    final String model;

    public DataStoreHolder(String fullyQualifiedName) {
        int dot = fullyQualifiedName.indexOf('.');
        if (dot < 2 || !(fullyQualifiedName.length() > dot))
            throw new IllegalArgumentException("Constructor argument not of form <MODEL>.<NAME>");
        model = fullyQualifiedName.substring(0, dot);
        name = fullyQualifiedName.substring(dot + 1, fullyQualifiedName.length());
    }

    public DataStoreHolder(String name, String model) {
        this.name = name;
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public String getModel() {
        return model;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((model == null) ? 0 : model.hashCode());
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
        DataStoreHolder other = (DataStoreHolder) obj;
        if (model == null) {
            if (other.model != null)
                return false;
        } else if (!model.equals(other.model))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}