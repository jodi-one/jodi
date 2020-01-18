package one.jodi.core.model;

import one.jodi.core.model.visitors.Visitable;

public interface Common extends Visitable {

    Common getParent();

}
