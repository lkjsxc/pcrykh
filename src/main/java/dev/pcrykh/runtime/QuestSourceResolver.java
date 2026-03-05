package dev.pcrykh.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class QuestSourceResolver {
    public List<Path> resolve(Path dataFolder, List<String> sources) {
        List<Path> resolved = new ArrayList<>();
        for (String source : sources) {
            Path path = dataFolder.resolve(source);
            if (!Files.exists(path)) {
                throw new ConfigException("Missing quest source: " + source);
            }
            if (Files.isDirectory(path)) {
                try {
                    List<Path> files = Files.walk(path)
                            .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json"))
                            .sorted(Comparator.comparing(Path::toString))
                            .collect(Collectors.toList());
                    resolved.addAll(files);
                } catch (IOException ex) {
                    throw new ConfigException("Failed to read quest source directory: " + source, ex);
                }
            } else {
                resolved.add(path);
            }
        }
        return resolved;
    }
}
