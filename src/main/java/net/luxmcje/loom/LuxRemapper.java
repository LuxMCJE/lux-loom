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
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                try (InputStream is = jarFile.getInputStream(entry)) {
                    memoryCache.put(entry.getName(), is.readAllBytes());
                }
            }
        }

        try (JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputJar)))) {
            for (var entry : memoryCache.entrySet()) {
                String name = entry.getKey();
                byte[] bytes = entry.getValue();

                if (name.endsWith(".class")) {
                    try {
                        ClassReader reader = new ClassReader(bytes);
                        ClassWriter writer = new ClassWriter(reader, 0); 
                        ClassVisitor cv = new ClassRemapper(writer, remapper);
                    
                        reader.accept(cv, 0);

                        byte[] remappedBytes = writer.toByteArray();
                        String internalName = name.replace(".class", "");
                        String mappedName = remapper.map(internalName);
                    
                        jos.putNextEntry(new JarEntry((mappedName != null ? mappedName : internalName) + ".class"));
                        jos.write(remappedBytes);
                    } catch (Exception e) {
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
