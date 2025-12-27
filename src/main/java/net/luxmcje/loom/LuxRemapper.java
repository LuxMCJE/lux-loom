package net.luxmcje.loom;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.nio.file.Files;
import java.util.List;

public class LuxRemapper {
    private final Map<String, String> mappingMap = new HashMap<>();

    public void loadTinyMappings(File tinyFile) throws IOException {
        List<String> lines = Files.readAllLines(tinyFile.toPath());
        for (String line : lines) {
            String[] parts = line.split("\t");
            if (parts[0].equals("CLASS")) {
                mappingMap.put(parts[1], parts[3]);
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
                byte[] bytes = jarFile.getInputStream(entry).readAllBytes();

                if (entry.getName().endsWith(".class")) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(0);
                    ClassVisitor cv = new ClassRemapper(writer, remapper);
                    reader.accept(cv, 0);
                    
                    String newName = mappingMap.getOrDefault(entry.getName().replace(".class", ""), 
                                                            entry.getName().replace(".class", "")) + ".class";
                    jos.putNextEntry(new JarEntry(newName));
                    jos.write(writer.toByteArray());
                } else {
                    jos.putNextEntry(new JarEntry(entry.getName()));
                    jos.write(bytes);
                }
            }
        }
    }
}
