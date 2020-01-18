package one.jodi.base.service.annotation;

public interface KeyParser {

    final static String NS_SCHEMA = "Schemas";
    final static String NS_VARIABLES = "Variables";
    final static String NS_TABLE = "Tables";
    final static String NS_COLUMN = "Columns";

    Key parseKey(String keyString);

}