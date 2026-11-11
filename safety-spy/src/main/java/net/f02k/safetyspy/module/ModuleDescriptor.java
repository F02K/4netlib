package net.f02k.safetyspy.module;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Parsed {@code module.yml} of a module jar.
 */
public record ModuleDescriptor(String name, String main, String description) {

    public static ModuleDescriptor read(ClassLoader loader) {
        try (InputStream in = loader.getResourceAsStream("module.yml")) {
            if (in == null) {
                throw new IllegalStateException("module.yml not found");
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            String name = yaml.getString("name");
            String main = yaml.getString("main");
            if (name == null || main == null) {
                throw new IllegalStateException("module.yml needs 'name' and 'main'");
            }
            return new ModuleDescriptor(name, main, yaml.getString("description", ""));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
