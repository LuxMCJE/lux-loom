package net.luxmcje.loom;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class LuxRemapper {
    private final Map<String, String> mappingMap = new HashMap<>();

    public void loadTinyMappings(File tinyFile) throws IOException {
        InputStream is = new FileInputStream(tinyFile);
        if (tinyFile.getName().endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\t");
                if (parts.length >= 3 && (parts[0].equals("CLASS") || parts[0].equals("c"))) {
                    mappingMap.put(parts[1], parts[parts.length - 1]);
                }
            }
        }
    }

    public void remapJar(File inputJar, File outputJar) throws IOException {
        SimpleRemapper remapper = new SimpleRemapper(mappingMap);
        Map<String, byte[]> memoryCache = new LinkedHashMap<>();

        try (JarFile jarFile = new JarFile(inputJar)) {
            jarFile.stream().forEach(entry -> {
                try (InputStream is = jarFile.getInputStream(entry)) {
                    memoryCache.put(entry.getName(), is.readAllBytes());
                } catch (IOException e) { e.printStackTrace(); }
            });
        }

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {
            for (var entry : memoryCache.entrySet()) {
                String name = entry.getKey();
                byte[] bytes = entry.getValue();

                if (name.endsWith(".class")) {
                    try {
                        ClassReader reader = new ClassReader(bytes);
                        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        ClassVisitor cv = new ClassRemapper(writer, remapper);
                    
                        reader.accept(cv, ClassReader.EXPAND_FRAMES);

                        byte[] remappedBytes = writer.toByteArray();
                        String mappedName = remapper.map(name.replace(".class", ""));
                        jos.putNextEntry(new JarEntry((mappedName == null ? name : mappedName + ".class")));
                        jos.write(remappedBytes);
                    } catch (Throwable t) {
                        System.err.println("[LuxLoom-Debug] Failed class: " + name + " | Reason: " + t.getMessage());
                        jos.putNextEntry(new JarEntry(name));
                        jos.write(bytes);
                    }
                } else {
                    jos.putNextEntry(new JarEntry(name));
                    jos.write(bytes);
                }
                jos.closeEntry();
            }
        }
    }
}
