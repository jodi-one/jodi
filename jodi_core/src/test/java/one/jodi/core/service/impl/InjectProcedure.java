package one.jodi.core.service.impl;

import java.nio.file.Path;
import java.util.Map;

public interface InjectProcedure<T> {

    void add(final Map<Path, T> procedures);

}
