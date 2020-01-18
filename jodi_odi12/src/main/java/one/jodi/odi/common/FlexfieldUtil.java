package one.jodi.odi.common;

import oracle.odi.domain.flexfields.IFlexFieldUser;

import java.util.Map;

public interface FlexfieldUtil<T extends IFlexFieldUser> {

    Map<String, Object> getFlexFieldValues(T odiObject);

    boolean existsFlexfieldValueByName(T odiObject, String flexFieldName);

    String getFlexFieldStringValueByName(T odiObject, String flexFieldName);

    long getFlexFieldLongValueByName(T odiObject, String flexFieldNam);

    void setFlexFields(T odiObject, String flexFieldName, String value);

    void setFlexFields(T odiObject, String flexFieldName, long value);

}
