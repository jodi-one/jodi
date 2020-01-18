package one.jodi.etl.internalmodel;

import java.util.List;

public interface Flow {

    String getName();

    int getOrder();

    Source getParent();

    List<OutputAttribute> getOutputAttributes();

}
