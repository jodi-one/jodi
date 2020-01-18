package one.jodi.core.service;

import one.jodi.base.model.types.DataStore;

/**
 * Validates the model being input.
 */
public interface ModelValidator {

    /**
     * Checks for Alternate key W_<TABLE_NAME>_D_U1.
     * * reason for it being is that without alternate key in the DMT layer,
     * <p>
     * one would run into:
     * see ODI-15050 http://docs.oracle.com/cd/E23943_01/core.1111/e10113/
     * chapter_odi_messages.htm
     * <p>
     * when using IKM Incremental Update.
     * <p>
     * ODI-15050: Flow Control not possible if no key is declared in your target
     * datastore
     * Cause: Flow control is enabled for the interface, but the target datastore
     * has no primary key defined.
     * Action: Define a primary key for the target datastore object, and re-add
     * the target datastore.
     * Level: 32
     * Type: ERROR
     * Impact: Programmatic
     *
     * @param properties
     * @param hasAlternativeKey
     * @return valid or not
     */
    boolean doCheck(final DataStore dataStore);
}
