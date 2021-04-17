package one.jodi.base.service.annotation;

public interface KeyParser {

    String NS_SCHEMA = "Schemas";
    String NS_VARIABLES = "Variables";
    String NS_TABLE = "Tables";
    String NS_COLUMN = "Columns";

    Key parseKey(String keyString);

}