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
                // دعم Tiny v1 (CLASS) و Tiny v2 (c)
                if (parts.length >= 3 && (parts[0].equals("CLASS") || parts[0].equals("c"))) {
                    String officialName = parts[1];
                    String namedName = parts[parts.length - 1];
                    mappingMap.put(officialName, namedName);
                }
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
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); 
                    ClassVisitor cv = new ClassRemapper(writer, remapper);
                    reader.accept(cv, 0);
    
                    String internalName = entry.getName().replace(".class", "");
                    String mappedName = remapper.map(internalName);
                    if (mappedName == null) {
                        mappedName = internalName;
                    }
                    String finalPath = mappedName + ".class";
    
                    jos.putNextEntry(new JarEntry(finalPath));
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
