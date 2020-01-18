package one.jodi.base.service.bi.ast;

import java.util.Date;

public interface DateNode extends ExprNode {
    Date getValue();

    // TODO remove this method
    String getUnderlyingValue();

}
