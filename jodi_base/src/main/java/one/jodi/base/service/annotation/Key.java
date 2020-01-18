package one.jodi.base.service.annotation;

import java.util.List;

class Key {
    private final List<NameSpaceComponent> nameSpace;

    public Key(final List<NameSpaceComponent> nameSpace) {
        super();
        this.nameSpace = nameSpace;
    }

    public List<NameSpaceComponent> getNameSpace() {
        return nameSpace;
    }

    public String getNameKey() {
        String sep = "";
        StringBuilder sb = new StringBuilder();
        for (NameSpaceComponent c : this.nameSpace) {
            sb.append(sep)
                    .append(c.getName());
            sep = ".";
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        String sep = "";
        StringBuilder sb = new StringBuilder();
        for (NameSpaceComponent c : this.nameSpace) {
            sb.append(sep)
                    .append(c.getType()
                            .getComponentName())
                    .append(".")
                    .append(c.getName());
            sep = ".";
        }
        return sb.toString();
    }
}