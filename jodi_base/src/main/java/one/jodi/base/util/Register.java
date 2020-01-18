package one.jodi.base.util;

import one.jodi.base.error.ErrorWarningMessageJodi;

/**
 * Used to register a cache to query and modify the cache
 */
public interface Register {

    void register(final Resource resource);

    void register(final ErrorWarningMessageJodi errorWarningMessage);
}
