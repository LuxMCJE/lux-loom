package net.luxmcje.loom;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class MinecraftDownloader {
    private static final String VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    public static void downloadClient(String version, Path outputPath) throws Exception {
        JsonObject manifest = JsonParser.parseReader(new InputStreamReader(new URL(VERSION_MANIFEST).openStream())).getAsJsonObject();
        String versionUrl = "";

        for (var v : manifest.getAsJsonArray("versions")) {
            if (v.getAsJsonObject().get("id").getAsString().equals(version)) {
                versionUrl = v.getAsJsonObject().get("url").getAsString();
                break;
            }
        }

        JsonObject versionJson = JsonParser.parseReader(new InputStreamReader(new URL(versionUrl).openStream())).getAsJsonObject();
        String clientUrl = versionJson.getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();

        try (var in = new URL(clientUrl).openStream()) {
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
