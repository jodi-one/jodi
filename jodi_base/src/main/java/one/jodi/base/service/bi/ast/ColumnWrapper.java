package one.jodi.base.service.bi.ast;

/**
 * This class represents an alias or logical column. This interface
 * must not be implemented by Spoofax module but is supplied by the
 * implementation.
 * <p>
 * The class is also used by implementation to identify the proper column. This
 * will be realized through an alternative interface that is not shared with the
 * Spoofax implementation.
 *
 */
public interface ColumnWrapper {

    String getTableName();

    String getName();

    String getType();

}
