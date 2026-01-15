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

        try (JarFile jarFile = new JarFile(inputJar);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (entry.isDirectory()) continue;
                
                if (name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA"))) {
                    continue;
                }

                byte[] bytes = jarFile.getInputStream(entry).readAllBytes();

                if (name.endsWith(".class")) {
                    try {
                        ClassReader reader = new ClassReader(bytes);
                        
                        ClassWriter writer = new ClassWriter(reader, 0);

                        ClassVisitor cv = new ClassRemapper(writer, remapper);
        
                        reader.accept(cv, 0); 

                        byte[] remappedBytes = writer.toByteArray();
        
                        String internalName = name.replace(".class", "");
                        
                        String mappedName = remapper.map(internalName);
                        if (mappedName == null) mappedName = internalName;
        
                        jos.putNextEntry(new JarEntry(mappedName + ".class"));
                        jos.write(remappedBytes);
                    } catch (Exception e) {
                        jos.putNextEntry(new JarEntry(name));
                        jos.write(bytes);
                    }
                } else {
                    if (!name.equals("META-INF/MANIFEST.MF")) {
                        jos.putNextEntry(new JarEntry(name));
                        jos.write(bytes);
                    }
                }
                jos.closeEntry();
            }
        }
    }  
}
