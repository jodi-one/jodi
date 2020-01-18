package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.NativeSequence;

public class NativeSequenceImpl extends SequenceImpl
        implements NativeSequence {

    private String schema;
    private String nativeName;

    public NativeSequenceImpl(final String name, final Integer increment,
                              final Boolean global, final String schema,
                              final String nativeName) {
        super(name, increment, global);
        this.schema = schema;
        this.nativeName = nativeName;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Override
    public String getNativeName() {
        return nativeName;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }
}
