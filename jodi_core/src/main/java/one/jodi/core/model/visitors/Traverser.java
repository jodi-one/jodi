//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference ementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.10.26 at 10:02:08 AM EDT 
//


package one.jodi.core.model.visitors;

import one.jodi.core.model.*;

public interface Traverser {


    public void traverse(Dataset aBean, Visitor aVisitor);

    public void traverse(Datasets aBean, Visitor aVisitor);

    public void traverse(FlowType aBean, Visitor aVisitor);

    public void traverse(FlowsType aBean, Visitor aVisitor);

    public void traverse(KmOption aBean, Visitor aVisitor);

    public void traverse(KmOptions aBean, Visitor aVisitor);

    public void traverse(KmType aBean, Visitor aVisitor);

    public void traverse(Lookup aBean, Visitor aVisitor);

    public void traverse(Lookups aBean, Visitor aVisitor);

    public void traverse(MappingExpressions aBean, Visitor aVisitor);

    public void traverse(Mappings aBean, Visitor aVisitor);

    public void traverse(ColumnType aBean, Visitor aVisitor);

    public void traverse(PivotType aBean, Visitor aVisitor);

    public void traverse(Properties aBean, Visitor aVisitor);

    public void traverse(Source aBean, Visitor aVisitor);

    public void traverse(Targetcolumn aBean, Visitor aVisitor);

    public void traverse(Transformation aBean, Visitor aVisitor);

    public void traverse(UnPivotType aBean, Visitor aVisitor);

    public void traverse(SubQueryType aBean, Visitor aVisitor);

    public void traverse(SyntheticRowType aBean, Visitor aAvisitor);
}
