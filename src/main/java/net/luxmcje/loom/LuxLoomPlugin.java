package net.luxmcje.loom;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;

public class LuxLoomPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getLogger().lifecycle("---------------------------------------");
        project.getLogger().lifecycle("   Initializing LuxLoom Toolchain      ");
        project.getLogger().lifecycle("---------------------------------------");

        Configuration minecraftConfig = project.getConfigurations().maybeCreate("minecraft");
        Configuration mappingsConfig = project.getConfigurations().maybeCreate("mappings");
        
        minecraftConfig.setCanBeResolved(true);
        mappingsConfig.setCanBeResolved(true);

        project.getRepositories().maven(repo -> {
            repo.setName("LuxRepo");
            repo.setUrl("https://luxmcje.github.io/lux-mappings/");
        });

        project.getRepositories().maven(repo -> {
            repo.setName("Minecraft");
            repo.setUrl("https://libraries.minecraft.net/");
        });

        project.afterEvaluate(p -> {
            String mcVersion = "1.20.1";
            Path cacheDir = project.getLayout().getBuildDirectory().getAsFile().get().toPath().resolve("lux-cache");
            Path rawClient = cacheDir.resolve("minecraft-" + mcVersion + "-raw.jar");
            Path mappedClient = cacheDir.resolve("minecraft-" + mcVersion + "-lux.jar");
            Path sourcesJar = cacheDir.resolve("minecraft-" + mcVersion + "-sources.jar");

            try {
                Files.createDirectories(cacheDir);
        
                if (!Files.exists(rawClient)) {
                    project.getLogger().lifecycle("[LuxLoom] Downloading Minecraft " + mcVersion);
                    MinecraftDownloader.downloadClient(mcVersion, rawClient);
                }

                if (!Files.exists(mappedClient)) {
                    project.getLogger().lifecycle("[LuxLoom] Remapping Minecraft to Lux Mappings");
                    File mappingFile = project.getConfigurations().getByName("mappings").getSingleFile();
                    
                    LuxRemapper remapper = new LuxRemapper();
                    remapper.loadTinyMappings(mappingFile);
                    remapper.remapJar(rawClient.toFile(), mappedClient.toFile());
                }

                if (!Files.exists(sourcesJar)) {
                   project.getLogger().lifecycle("[LuxLoom] Decompiling Minecraft...");
                   LuxDecompiler.decompile(mappedClient, sourcesJar);
                }

                project.getDependencies().add("implementation", project.files(mappedClient));
                project.getLogger().lifecycle("[LuxLoom] Environment is ready!");

            } catch (Exception e) {
                project.getLogger().error("LuxLoom failed to prepare environment", e);
            }
        });
    }
}
