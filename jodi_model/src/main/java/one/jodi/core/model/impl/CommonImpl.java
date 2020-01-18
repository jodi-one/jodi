package one.jodi.core.model.impl;


import one.jodi.core.model.Common;
import one.jodi.core.model.visitors.Visitor;

@javax.xml.bind.annotation.XmlTransient
public abstract class CommonImpl implements Common {

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


    @Override
    public void accept(Visitor visitor) {


    }

}