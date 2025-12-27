package net.luxmcje.loom;

import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.io.File;

public class LuxDecompiler {
    public static void decompile(Path inputJar, Path outputJar) {
        Map<String, Object> options = new HashMap<>();
        options.put(IFernflowerPreferences.INDENT_STRING, "    ");
        options.put(IFernflowerPreferences.REMOVE_BRIDGE, "1");

        IFernflowerLogger logger = new IFernflowerLogger() {
            @Override public void writeMessage(String message, Severity severity) {}
            @Override public void writeMessage(String message, Severity severity, Throwable t) {}
        };

        ConsoleDecompiler decompiler = new ConsoleDecompiler(outputJar.getParent().toFile(), options, logger);
        decompiler.addSource(inputJar.toFile());
        decompiler.decompileContext();
    }
}
