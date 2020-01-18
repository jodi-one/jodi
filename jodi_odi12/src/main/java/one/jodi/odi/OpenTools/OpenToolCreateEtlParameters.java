package one.jodi.odi.OpenTools;

import oracle.odi.sdk.opentools.IOpenToolParameter;

public class OpenToolCreateEtlParameters implements IOpenToolParameter {

    String code;
    String help;
    String name;

    public OpenToolCreateEtlParameters(String string, String string1, String string2) {
        code = string;
        help = string2;
        name = string1;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(String string) {
        code = string;
    }

    @Override
    public String getHelp() {
        return help;
    }

    @Override
    public void setHelp(String string) {
        help = string;
    }

    @Override
    public boolean isMandatory() {
        return false;
    }

    @Override
    public void setMandatory(boolean b) {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String string) {
        name = string;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void setValue(Object object) {
    }
}
