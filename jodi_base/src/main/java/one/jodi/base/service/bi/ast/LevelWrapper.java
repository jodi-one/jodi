package one.jodi.base.service.bi.ast;

/**
 * This class represents and identified logical drill down key. This class is
 * used by implementation to identify a drill down key that is only available
 * in the implementation.
 *
 */
public interface LevelWrapper {

    String getDimensionName();

    String getName();

}
