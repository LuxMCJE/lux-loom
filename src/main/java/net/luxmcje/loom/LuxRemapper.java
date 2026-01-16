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
        Map<String, byte[]> outEntries = new HashMap<>();

        try (JarFile jarFile = new JarFile(inputJar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) continue;

                byte[] bytes;
                try (InputStream is = jarFile.getInputStream(entry)) {
                    bytes = is.readAllBytes();
                }

                if (name.endsWith(".class")) {
                    try {
                        ClassReader reader = new ClassReader(bytes);
                        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                        ClassVisitor cv = new ClassRemapper(writer, remapper);
                        reader.accept(cv, 0);

                        byte[] remappedBytes = writer.toByteArray();
                        String mappedName = remapper.map(name.replace(".class", ""));
                        if (mappedName == null) mappedName = name.replace(".class", "");
                    
                        outEntries.put(mappedName + ".class", remappedBytes);
                    } catch (Exception e) {
                        System.err.println("Skipping corrupted class: " + name);
                        outEntries.put(name, bytes);
                    }
                } else if (!name.startsWith("META-INF/")) {
                    outEntries.put(name, bytes);
                }
            }
        }

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {
            for (Map.Entry<String, byte[]> entry : outEntries.entrySet()) {
                JarEntry je = new JarEntry(entry.getKey());
                jos.putNextEntry(je);
                jos.write(entry.getValue());
                jos.closeEntry();
            }
        }
    }
}
