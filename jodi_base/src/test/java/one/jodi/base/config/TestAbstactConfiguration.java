package one.jodi.base.config;

import one.jodi.base.error.ErrorWarningMessageJodi;

/**
 *
 */
public class TestAbstactConfiguration extends AbstactConfiguration {

    TestAbstactConfiguration(ErrorWarningMessageJodi errorWarningMessages, String propFile,
                             boolean usedForTesting) {
        super(errorWarningMessages, propFile, usedForTesting);
    }
}
