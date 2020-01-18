package one.jodi.core.service.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.util.CollectXmlObjectsUtilImpl;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class TestCollectXmlObjectsUtil<T, O> extends CollectXmlObjectsUtilImpl<T, O>
        implements InjectProcedure<T> {

    private Map<Path, T> procedures = new LinkedHashMap<>();

    TestCollectXmlObjectsUtil(final Class<O> objectFactory,
                              final String xsdFileName,
                              final ErrorWarningMessageJodi errorWarningMessages) {
        super(objectFactory, xsdFileName, errorWarningMessages);
    }

    @Override
    protected Optional<Entry<Path, T>> loadXmlFile(final Path f) {
        Iterator<Entry<Path, T>> iterator = procedures.entrySet().iterator();
        Entry<Path, T> entry = null;
        if (iterator.hasNext()) {
            entry = iterator.next();
            procedures.remove(entry.getKey());
        }

        return Optional.ofNullable(entry);
    }

    @Override
    public void add(final Map<Path, T> procedures) {
        this.procedures.putAll(procedures);
    }

}