package net.luxmcje.loom;

import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

public class LuxDecompiler {
    public static void decompile(Path inputJar, Path outputJar) {
        Map<String, Object> options = new HashMap<>();
        options.put("ind", "    ");
        options.put("rbr", "1");

        IBytecodeProvider provider = (externalPath, internalPath) -> {
            try (InputStream is = new FileInputStream(externalPath)) {
                return is.readAllBytes();
            }
        };

        IResultSaver saver = new IResultSaver() {
            @Override public void saveClassFile(String path, String clsName, String entryName, String content, int[] mapping) {}
            @Override public void saveFolder(String path) {}
            @Override public void copyFile(String source, String path, String entryName) {}
            @Override public void createArchive(String path, String archiveName, Manifest manifest) {}
            @Override public void saveDirEntry(String path, String archiveName, String entryName) {}
            @Override public void copyEntry(String source, String path, String archiveName, String entryName) {}
            @Override public void saveClassEntry(String path, String archiveName, String clsName, String entryName, String content) {
                try {
                    File out = outputJar.toFile();
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override public void closeArchive(String path, String archiveName) {}
        };

        Fernflower engine = new Fernflower(provider, saver, options, new PrintStreamLogger(System.out));
        engine.addSource(inputJar.toFile());
        engine.decompileContext();
    }
}
