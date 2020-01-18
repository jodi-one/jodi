//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference ementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.10.26 at 10:02:08 AM EDT 
//


package one.jodi.core.model.visitors;

import one.jodi.core.model.*;

public class TraversingVisitor
        implements Visitor {

    private boolean traverseFirst;
    private Visitor visitor;
    private Traverser traverser;
    private TraversingVisitorProgressMonitor progressMonitor;

    public TraversingVisitor(Traverser aTraverser, Visitor aVisitor) {
        traverser = aTraverser;
        visitor = aVisitor;
    }

    public boolean getTraverseFirst() {
        return traverseFirst;
    }

    public void setTraverseFirst(boolean aVisitor) {
        traverseFirst = aVisitor;
    }

    public Visitor getVisitor() {
        return visitor;
    }

    public void setVisitor(Visitor aVisitor) {
        visitor = aVisitor;
    }

    public Traverser getTraverser() {
        return traverser;
    }

    public void setTraverser(Traverser aVisitor) {
        traverser = aVisitor;
    }

    public TraversingVisitorProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }

    public void setProgressMonitor(TraversingVisitorProgressMonitor aVisitor) {
        progressMonitor = aVisitor;
    }

    public void visit(Dataset aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(Datasets aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(FlowType aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(FlowsType aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(KmOption aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(KmOptions aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(KmType aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(Lookup aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(Lookups aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(MappingExpressions aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(Mappings aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(ColumnType aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(PivotType aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(Properties aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(Source aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(Targetcolumn aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(Transformation aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    public void visit(UnPivotType aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }

    @Override
    public void visit(MappingCommandType aBean) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visit(SubQueryType aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }


    @Override
    public void visit(SyntheticRowType aBean) {
        if (traverseFirst == true) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
        aBean.accept(getVisitor());
        if (progressMonitor != null) {
            progressMonitor.visited(aBean);
        }
        if (traverseFirst == false) {
            getTraverser().traverse(aBean, this);
            if (progressMonitor != null) {
                progressMonitor.traversed(aBean);
            }
        }
    }
}
