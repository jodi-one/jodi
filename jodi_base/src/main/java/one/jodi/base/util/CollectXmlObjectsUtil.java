package one.jodi.base.util;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface CollectXmlObjectsUtil<T> {

    Map<Path, T> collectObjectsFromFiles(List<Path> files);

}