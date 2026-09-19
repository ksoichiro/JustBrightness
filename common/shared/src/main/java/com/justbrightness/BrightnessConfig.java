package com.justbrightness;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class BrightnessConfig {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final double MIN_GAMMA = 1.0;
    public static final double MAX_GAMMA = 32.0;
    public static final double DEFAULT_GAMMA = 16.0;
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_SHOW_TOGGLE_MESSAGE = true;

    private static final String FILE_NAME = "justbrightness.toml";
    private static final String DEFAULT_CONFIG_RESOURCE = "/justbrightness-default-config.toml";
    private static final Logger LOGGER = LogManager.getLogger(JustBrightness.MOD_ID);

    private static Path configFile;
    private static double gamma = DEFAULT_GAMMA;
    private static boolean defaultEnabled = DEFAULT_ENABLED;
    private static boolean showToggleMessage = DEFAULT_SHOW_TOGGLE_MESSAGE;

    private BrightnessConfig() {
    }

    public static double getGamma() {
        return gamma;
    }

    public static void setGamma(double value) {
        gamma = Math.max(MIN_GAMMA, Math.min(MAX_GAMMA, value));
    }

    public static boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public static void setDefaultEnabled(boolean value) {
        defaultEnabled = value;
    }

    public static boolean isToggleMessageEnabled() {
        return showToggleMessage;
    }

    public static void setToggleMessageEnabled(boolean value) {
        showToggleMessage = value;
    }

    public static void load(Path configDir) {
        configFile = configDir.resolve(FILE_NAME);
        if (!Files.exists(configFile)) {
            ensureFileExists(configFile);
        }

        try (InputStream in = Files.newInputStream(configFile)) {
            CommentedConfig parsed = new TomlParser().parse(in);
            Object gammaValue = parsed.get("gamma");
            if (gammaValue instanceof Number number) {
                setGamma(number.doubleValue());
            } else if (gammaValue != null) {
                LOGGER.warn("Invalid gamma = {} in {}; using default", gammaValue, FILE_NAME);
            }
            Object defaultEnabledValue = parsed.get("default_enabled");
            if (defaultEnabledValue instanceof Boolean bool) {
                defaultEnabled = bool;
            } else if (defaultEnabledValue != null) {
                LOGGER.warn("Invalid default_enabled = {} in {}; using default", defaultEnabledValue, FILE_NAME);
            }
            Object showToggleMessageValue = parsed.get("show_toggle_message");
            if (showToggleMessageValue instanceof Boolean bool) {
                showToggleMessage = bool;
            } else if (showToggleMessageValue != null) {
                LOGGER.warn("Invalid show_toggle_message = {} in {}; using default", showToggleMessageValue, FILE_NAME);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Failed to read {}; using defaults", FILE_NAME, e);
            gamma = DEFAULT_GAMMA;
            defaultEnabled = DEFAULT_ENABLED;
            showToggleMessage = DEFAULT_SHOW_TOGGLE_MESSAGE;
        }
    }

    private static void ensureFileExists(Path configFile) {
        try {
            Files.createDirectories(configFile.getParent());
            try (InputStream in = BrightnessConfig.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
                if (in == null) {
                    throw new IOException("Bundled default config resource not found: " + DEFAULT_CONFIG_RESOURCE);
                }
                Files.copy(in, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to write default config at {}; falling back to generating one", configFile, e);
            save();
        }
    }

    public static void save() {
        if (configFile == null) {
            LOGGER.warn("Config was never loaded; skipping save");
            return;
        }
        try {
            Files.createDirectories(configFile.getParent());
            try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(configFile, TomlFormat.instance()).build()) {
                if (Files.exists(configFile)) {
                    fileConfig.load();
                }
                fileConfig.set("schema_version", CURRENT_SCHEMA_VERSION);
                fileConfig.set("gamma", gamma);
                fileConfig.set("default_enabled", defaultEnabled);
                fileConfig.set("show_toggle_message", showToggleMessage);
                fileConfig.save();
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to save {}", FILE_NAME, e);
        }
    }
}
