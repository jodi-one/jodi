package one.jodi.base.service.bi;

import one.jodi.base.service.bi.finder.ColumnFinder;
import one.jodi.base.service.bi.finder.VariableMatcher;

/**
 * This class is implemented by Spoofax module to provide the parser.
 */
public interface BiExpressionParser {

    /**
     * This method is only called by the Generator to process an expression.
     * The specific context of the expression is supplied within the implementation
     * of the ColumnFinder.
     *
     * @param expression      BI expression to be parsed
     * @param finder          supplied by Generator to enable parser implementation to
     *                        determine if a column exists as defined or
     * @param variableMatcher TODO
     * @param isLogical       if true indicates that the expression operates on physical
     *                        columns and is executed in the pre-aggregation phase. Otherwise,
     *                        the expression operates on logical columns and is executed in
     *                        the post-aggregation phase.
     * @param checkType       TODO
     * @return result of the parse processing, which is either an AST or error
     * messages
     */
    ParseResult parse(String expression, ColumnFinder finder,
                      VariableMatcher variableMatcher, boolean isLogical,
                      boolean checkType);
}
