package one.jodi.base.service.annotation;

import com.google.inject.Inject;
import one.jodi.base.config.BaseConfigurations;
import one.jodi.base.service.metadata.ColumnMetaData;
import one.jodi.base.service.metadata.DataStoreDescriptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TableBusinessRulesImpl implements TableBusinessRules {

    private final Pattern ABBREVIATION_PATTERN;
    private final BaseConfigurations biProperties;

    @Inject
    public TableBusinessRulesImpl(final BaseConfigurations biProperties) {
        super();
        this.biProperties = biProperties;

        //cache abbreviation pattern
        String regex = biProperties.getAbbreviationPattern();
        this.ABBREVIATION_PATTERN = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    private int findJsMetadata(final String text) {
        int jsonBegin = -1;
        int jsonindexEnd = text.lastIndexOf("}");
        if (jsonindexEnd <= 0 || !text.substring(jsonindexEnd + 1).trim().isEmpty()) {
            return -1;
        }

        // find index that
        int closedCount = 1;
        for (int i = jsonindexEnd - 1; i >= 0; i--) {
            if (text.charAt(i) == '{') {
                closedCount--;
            } else if (text.charAt(i) == '}') {
                closedCount++;
            }
            if (closedCount == 0) {
                jsonBegin = i;
                break;
            }
        }

        // indicates that bracket was not properly opened
        if (jsonBegin < 0) {
            return -2;
        }

        return jsonBegin;
    }

    private String extractExtendedMetadata(final String text) {
        String extendedMetadata = "";

        if (text == null) {
            return extendedMetadata;
        }

        int index = text.indexOf(biProperties.getMetadataSeparator());
        if (index > 0) {
            extendedMetadata = text.substring(0, index).trim();
        }
        return extendedMetadata;
    }

    private String extractDescription(final String text) {
        if (text == null) {
            return "";
        }
        String documentation;
        int index = text.indexOf(biProperties.getMetadataSeparator());
        if (index >= 0) {
            documentation = text.substring(biProperties.getMetadataSeparator().length() + index);
        } else {
            documentation = text;
        }

        // remove JASON meta data
        int jsIndex = findJsMetadata(documentation);
        if (jsIndex >= 0) {
            documentation = documentation.substring(0, jsIndex);
        }

        return documentation.trim();
    }

    private String extractAbbreviation(final String text) {
        String abbreviation = "";
        if (text == null) {
            return abbreviation;
        }

        Matcher m = ABBREVIATION_PATTERN.matcher(text);
        if (m.find()) {
            abbreviation = m.group(1);
        }

        return abbreviation.trim();
    }


    @Override
    public String getDescription(final DataStoreDescriptor dataStore) {
        return extractDescription(dataStore.getDescription());
    }

    @Override
    public String getDescription(final ColumnMetaData column) {
        return extractDescription(column.getDescription());
    }

    private String extractMetadata(final String metadata) {
        String text = extractExtendedMetadata(metadata);
        Matcher m = ABBREVIATION_PATTERN.matcher(text);
        if (m.find()) {
            text = text.substring(0, m.start()).trim();
        }

        return text;
    }

    @Override
    public String getExtendedMetadata(final DataStoreDescriptor dataStore) {
        return extractMetadata(dataStore.getDescription());
    }

    @Override
    public String getExtendedMetadata(final ColumnMetaData column) {
        return extractMetadata(column.getDescription());
    }

    @Override
    public String getAbbreviatedMetadata(final DataStoreDescriptor dataStore) {
        return extractAbbreviation(extractExtendedMetadata(dataStore.getDescription()));
    }

    @Override
    public String getAbbreviatedMetadata(final ColumnMetaData column) {
        return extractAbbreviation(extractExtendedMetadata(column.getDescription()));
    }

    private String getJsMetadata(final String text) {
        String found = "";
        int index = findJsMetadata(text);
        if (index >= 0) {
            found = text.substring(index).trim();
        }
        return found;
    }

    @Override
    public String getMetadata(final DataStoreDescriptor dataStore) {
        String result = "";
        if (dataStore.getDescription() != null) {
            result = getJsMetadata(dataStore.getDescription());
        }
        return result;
    }

    @Override
    public String getMetadata(final ColumnMetaData column) {
        String result = "";
        if (column.getDescription() != null) {
            result = getJsMetadata(column.getDescription());
        }
        return result;
    }

}
