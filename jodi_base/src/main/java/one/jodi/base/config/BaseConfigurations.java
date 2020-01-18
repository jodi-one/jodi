package one.jodi.base.config;

import java.util.List;

public interface BaseConfigurations {

    //
    // Schema processing
    //

    String getOdbStarUrl();

    String getOdbStarUsername();

    List<String> getTableExclusionPattern();

    List<String> getTableInclusionPattern();

    //
    // Metadata and Annotations
    //

    boolean continueOnAnnotationFailure();

    String getMetadataSeparator();

    String getAbbreviationPattern();

    //
    // Dimensions
    //

    String getDimensionDDLFileName();

    String getLocationDimensionXSD();

    String getExternalDimensionPrefix();

}
