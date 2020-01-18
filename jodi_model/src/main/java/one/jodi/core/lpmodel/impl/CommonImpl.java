package one.jodi.core.lpmodel.impl;

import one.jodi.core.lpmodel.Common;

public class CommonImpl implements Common {

    @javax.xml.bind.annotation.XmlTransient
    protected Common parent;

    @SuppressWarnings("rawtypes")
    public void afterUnmarshal(javax.xml.bind.Unmarshaller u, Object parent) {
        try {
            this.parent = (Common) parent;
        } catch (ClassCastException cce) {
            this.parent = (Common) (((javax.xml.bind.JAXBElement) parent).getValue());
        }
    }

    public Common getParent() {
        return parent;
    }

}
