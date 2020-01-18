package one.jodi.qa.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileModifier {

    public static void readAndModifyFile(String metadataDir, String variablesFile, String namePrefix) throws IOException {
        Path file = Paths.get(metadataDir, variablesFile);
        List<String> lines = Files.readAllLines(file);
        StringBuilder buffer = new StringBuilder();
        for (String line : lines) {
            if (line.contains("<Name>") && !line.contains(namePrefix)) {
                buffer.append(line.replace("<Name>", "<Name>" + namePrefix));
            } else {
                buffer.append(line);
            }
            buffer.append(System.getProperty("line.separator"));
        }
        Files.write(Paths.get(metadataDir, variablesFile), buffer.toString().getBytes());
    }
}
