package one.jodi.core.extensions.types;

public interface TargetColumnFlags {


    Boolean isInsert();

    Boolean isUpdate();

    Boolean isUpdateKey();

    Boolean isMandatory();

    Boolean useExpression();


}
