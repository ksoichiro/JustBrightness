package com.justbrightness;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BrightnessConfig {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final double MIN_GAMMA = 1.0;
    public static final double MAX_GAMMA = 32.0;
    public static final double DEFAULT_GAMMA = 16.0;

    private static final String FILE_NAME = "justbrightness.json";
    private static final Logger LOGGER = LogManager.getLogger(JustBrightness.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configFile;
    private static double gamma = DEFAULT_GAMMA;
    private static boolean defaultEnabled = false;

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

    public static void load(Path configDir) {
        configFile = configDir.resolve(FILE_NAME);
        if (!Files.exists(configFile)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(configFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                LOGGER.warn("{} is empty; using defaults", FILE_NAME);
                return;
            }
            if (json.has("gamma")) {
                setGamma(json.get("gamma").getAsDouble());
            }
            if (json.has("default_enabled")) {
                defaultEnabled = json.get("default_enabled").getAsBoolean();
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Failed to read {}; using defaults", FILE_NAME, e);
            gamma = DEFAULT_GAMMA;
            defaultEnabled = false;
        }
    }

    public static void save() {
        if (configFile == null) {
            LOGGER.warn("Config was never loaded; skipping save");
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("schema_version", CURRENT_SCHEMA_VERSION);
        json.addProperty("gamma", gamma);
        json.addProperty("default_enabled", defaultEnabled);
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to save {}", FILE_NAME, e);
        }
    }
}
