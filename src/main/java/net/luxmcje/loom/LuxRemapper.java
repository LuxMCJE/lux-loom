package net.luxmcje.loom;

import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import java.io.File;
import java.nio.file.Path;

public class LuxRemapper {

    public static void remap(Path inputJar, Path outputJar, Path mappingFile) {
        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(TinyUtils.createTinyMappingProvider(mappingFile, "official", "named"))
                .build();

        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(outputJar).build {
            remapper.readInputs(inputJar);
            
            remapper.apply(outputConsumer);
            remapper.finish();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            remapper.finish();
        }
    }
}
