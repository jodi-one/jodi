package one.jodi.etl.internalmodel.impl;

final class Translation {
    private final String key;
    private final String translation;

    protected Translation(final String key, final String translation) {
        this.key = key;
        this.translation = translation;
    }

    public String getKey() {
        return key;
    }

    public String getTranslation() {
        return translation;
    }
}
