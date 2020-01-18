package one.jodi.base.service.files;

import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.List;

public abstract class FileCollectorVisitor extends SimpleFileVisitor<Path> {

    public abstract List<Path> getPathList();

}
