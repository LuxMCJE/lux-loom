package net.luxmcje.loom;

import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LuxDecompiler {
    public static void decompile(Path inputJar, Path outputJar) {
        Map<String, Object> options = new HashMap<>();
        options.put(IFernflowerPreferences.INDENT_STRING, "    ");
        options.put(IFernflowerPreferences.REMOVE_BRIDGE, "1");
        options.put(IFernflowerPreferences.ASCII_STRING_CHARACTERS, "1");

        ConsoleDecompiler decompiler = new ConsoleDecompiler(outputJar.getParent().toFile(), options);
        decompiler.addSource(inputJar.toFile());
        
        decompiler.decompileContext();
    }
}
