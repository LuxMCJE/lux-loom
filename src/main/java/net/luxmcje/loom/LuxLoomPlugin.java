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
    }
}
