package one.jodi.core.extensions.types;

import one.jodi.etl.km.KnowledgeModuleType;

import java.util.Set;


/**
 * Data Transfer interface used to describe a Knowledge Module of a particular name and type along with options
 * used therein.
 */
public interface KnowledgeModuleConfiguration {

    public static final KnowledgeModuleConfiguration Null = new KnowledgeModuleConfiguration() {
        @Override
        public String getName() {
            return null;
        }

        @Override
        public KnowledgeModuleType getType() {
            return KnowledgeModuleType.Unknown;
        }

        @Override
        public Set<String> getOptionKeys() {
            return null;
        }

        @Override
        public Object getOptionValue(String option) {
            return null;
        }
    };


    public abstract String getName();

    public abstract KnowledgeModuleType getType();

    public abstract Set<String> getOptionKeys();

    public abstract Object getOptionValue(String option);

}