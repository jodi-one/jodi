package one.jodi.base.service.files;

import java.nio.file.Path;
import java.util.List;

public interface FileCollector {

    List<Path> collectInPath(Path path, FileCollectorVisitor visitor);

    List<Path> collectInPath(Path path, String startsWith, String endsWith,
                             String orFilenameIs);
}
