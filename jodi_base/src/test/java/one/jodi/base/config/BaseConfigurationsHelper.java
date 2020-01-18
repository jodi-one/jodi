package one.jodi.base.config;

import java.util.Collections;
import java.util.List;

public class BaseConfigurationsHelper {

    private static BaseConfigurations baseConfiguratons = null;

    private static BaseConfigurations createBaseConfiguration() {
        BaseConfigurations baseConfigurations = new BaseConfigurations() {

            @Override
            public String getOdbStarUrl() {
                return null;
            }

            @Override
            public String getOdbStarUsername() {
                return null;
            }

            @Override
            public List<String> getTableExclusionPattern() {
                return Collections.emptyList();
            }

            @Override
            public List<String> getTableInclusionPattern() {
                return Collections.singletonList("[\\w]+");
            }

            @Override
            public boolean continueOnAnnotationFailure() {
                return false;
            }

            @Override
            public String getMetadataSeparator() {
                return "---";
            }

            @Override
            public String getAbbreviationPattern() {
                return "\\(\\(([\\w\\d\\s#\\.]+)\\)\\)$";
            }

            @Override
            public String getDimensionDDLFileName() {
                return "dimensions.sql";
            }

            @Override
            public String getLocationDimensionXSD() {
                return "";
            }

            @Override
            public String getExternalDimensionPrefix() {
                return "dim_";
            }

        };

        return baseConfigurations;
    }

    // TODO reference to a base-only implementation
    public static BaseConfigurations getTestBaseConfigurations() {
        if (BaseConfigurationsHelper.baseConfiguratons == null) {
            baseConfiguratons = createBaseConfiguration();
        }
        return baseConfiguratons;
    }
}
