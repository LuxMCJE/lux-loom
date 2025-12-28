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

            try (InputStream is = jarFile.getInputStream(entry)) {
                byte[] bytes = is.readAllBytes();

                if (entry.getName().endsWith(".class")) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(reader, 0); 
                    ClassVisitor cv = new ClassRemapper(writer, remapper);
                    
                    reader.accept(cv, ClassReader.EXPAND_FRAMES);
                    
                    String internalName = entry.getName().replace(".class", "");
                    String mappedName = remapper.map(internalName);
                    if (mappedName == null) mappedName = internalName;
                    
                    jos.putNextEntry(new JarEntry(mappedName + ".class"));
                    jos.write(writer.toByteArray());
                } else {
                    if (!entry.getName().equals("META-INF/MANIFEST.MF")) {
                        jos.putNextEntry(new JarEntry(entry.getName()));
                        jos.write(bytes);
                        }
                    }
                jos.closeEntry();
                }
            }
        }
    }
}
