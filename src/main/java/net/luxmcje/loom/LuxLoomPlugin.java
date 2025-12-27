package net.luxmcje.loom;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class LuxLoomPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getLogger().lifecycle("---------------------------------------");
        project.getLogger().lifecycle("   Initializing LuxLoom Toolchain      ");
        project.getLogger().lifecycle("---------------------------------------");

        Configuration minecraftConfig = project.getConfigurations().create("minecraft");
        Configuration mappingsConfig = project.getConfigurations().create("mappings");
        
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

        project.getLogger().lifecycle("[LuxLoom] Repositories and Configurations initialized.");
        
        project.afterEvaluate(p -> {
            String mcVersion = "1.20.1";
            Path cacheDir = project.getBuildDir().toPath().resolve("lux-cache");
            Path rawClient = cacheDir.resolve("minecraft-" + mcVersion + "-raw.jar");
            Path mappedClient = cacheDir.resolve("minecraft-" + mcVersion + "-lux.jar");

            try {
                Files.createDirectories(cacheDir);
        
                if (!Files.exists(rawClient)) {
                    project.getLogger().lifecycle(":Downloading Minecraft " + mcVersion);
                    MinecraftDownloader.downloadClient(mcVersion, rawClient);
                 }

                if (!Files.exists(mappedClient)) {
                    project.getLogger().lifecycle(":Remapping Minecraft to Lux Mappings");
                    Path mappingFile = project.getConfigurations().getByName("mappings").getSingleFile().toPath();
                    LuxRemapper.remap(rawClient, mappedClient, mappingFile);
                }

                project.getDependencies().add("implementation", project.files(mappedClient));

            } catch (Exception e) {
                project.getLogger().error("LuxLoom failed to prepare environment", e);
            }
        });
    }
}
