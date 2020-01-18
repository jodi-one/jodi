package one.jodi.base.service.annotation;

class NameSpaceComponent {

    private final NameSpaceType type;
    private final String name;
    public NameSpaceComponent(final NameSpaceType type, final String name) {
        super();
        this.type = type;
        this.name = name;
    }

    public NameSpaceType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public enum NameSpaceType {
        SCHEMA(KeyParser.NS_SCHEMA),
        TABLE(KeyParser.NS_TABLE);

        private final String componentName;

        NameSpaceType(final String componentName) {
            this.componentName = componentName;
        }

        public String getComponentName() {
            return componentName;
        }

    }

}