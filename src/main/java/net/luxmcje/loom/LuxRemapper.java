package net.luxmcje.loom;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.nio.file.Files;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class LuxRemapper {
    private final Map<String, String> mappingMap = new HashMap<>();

    public void loadTinyMappings(File tinyFile) throws IOException {
        List<String> lines = Files.readAllLines(tinyFile.toPath(), StandardCharsets.UTF_8);
        
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("#")) continue;
            
            String[] parts = line.split("\t");
            if (parts.length >= 3 && (parts[0].equals("CLASS") || parts[0].equals("c"))) {
                String officialName = parts[1];
                String namedName = parts[parts.length - 1];
                mappingMap.put(officialName, namedName);
            }
        }
    }

    public void remapJar(File inputJar, File outputJar) throws IOException {
        SimpleRemapper remapper = new SimpleRemapper(mappingMap);
        
        try (JarFile jarFile = new JarFile(inputJar);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {
            
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
            
                if (entry.isDirectory()) continue;

                byte[] bytes = jarFile.getInputStream(entry).readAllBytes();

                if (entry.getName().endsWith(".class")) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(0);
                    ClassVisitor cv = new ClassRemapper(writer, remapper);
                    reader.accept(cv, 0);
                    
                    String internalName = entry.getName().replace(".class", "");
                    String remappedName = mappingMap.getOrDefault(internalName, internalName) + ".class";
                    
                    jos.putNextEntry(new JarEntry(remappedName));
                    jos.write(writer.toByteArray());
                } else {
                    jos.putNextEntry(new JarEntry(entry.getName()));
                    jos.write(bytes);
                }
                jos.closeEntry();
            }
        }
    }
}
