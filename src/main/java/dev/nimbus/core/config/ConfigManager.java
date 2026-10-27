package dev.nimbus.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.nimbus.core.Nimbus;
import dev.nimbus.core.module.Module;
import dev.nimbus.core.policy.PolicyClass;
import dev.nimbus.core.policy.PolicyOverride;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Layout:  config/nimbus/config.json            global (active profile, policy override)
 *          config/nimbus/profiles/NAME.json     per-profile module settings
 * Writes are atomic (temp file + rename). A corrupt file is renamed to *.bak and defaults are used.
 * GATED modules never persist "enabled=true": they always start off each session.
 */
public final class ConfigManager {
    public static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern PROFILE_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final int AUTOSAVE_DELAY_TICKS = 40;

    public static final class GlobalConfig {
        public int schema = SCHEMA_VERSION;
        public String activeProfile = "default";
        public PolicyOverride policyOverride = PolicyOverride.AUTO;
        public boolean authorizedAck = false;
    }

    private final Path root;
    private final Path profilesDir;
    private final Path globalFile;
    private GlobalConfig global = new GlobalConfig();
    private boolean dirty;
    private int dirtyTicks;
    private boolean loading;

    public ConfigManager(Path root) {
        this.root = root;
        this.profilesDir = root.resolve("profiles");
        this.globalFile = root.resolve("config.json");
    }

    public Path root() { return root; }
    public GlobalConfig global() { return global; }
    public static boolean isValidProfileName(String n) { return n != null && PROFILE_NAME.matcher(n).matches(); }
    private Path profileFile(String name) { return profilesDir.resolve(name + ".json"); }

    // ---------------- global ----------------
    public void loadGlobal() {
        Optional<JsonObject> json = readJson(globalFile);
        if (json.isEmpty()) { saveGlobal(); return; }
        try {
            GlobalConfig g = GSON.fromJson(json.get(), GlobalConfig.class);
            if (g == null) return;
            if (!isValidProfileName(g.activeProfile)) g.activeProfile = "default";
            if (g.policyOverride == null) g.policyOverride = PolicyOverride.AUTO;
            global = g;
        } catch (JsonParseException e) {
            Nimbus.LOG.warn("Bad config.json, using defaults: {}", e.toString());
            backupCorrupt(globalFile);
        }
    }

    public void saveGlobal() {
        global.schema = SCHEMA_VERSION;
        writeAtomic(globalFile, GSON.toJson(global));
    }

    // ---------------- profiles ----------------
    public void loadProfile(String name) {
        if (!isValidProfileName(name)) name = "default";
        loading = true;
        try {
            JsonObject root = readJson(profileFile(name)).map(this::migrate).orElse(null);
            JsonObject mods = root != null && root.has("modules") && root.get("modules").isJsonObject()
                ? root.getAsJsonObject("modules") : new JsonObject();

            for (Module m : Nimbus.modules().all()) {
                m.resetSettings();
                JsonObject mj = mods.has(m.id()) && mods.get(m.id()).isJsonObject() ? mods.getAsJsonObject(m.id()) : null;
                if (mj != null && mj.has("settings") && mj.get("settings").isJsonObject())
                    m.settingsFromJson(mj.getAsJsonObject("settings"));

                boolean want = m.enabledByDefault();
                if (mj != null && mj.has("enabled")) {
                    try { want = mj.get("enabled").getAsBoolean(); } catch (RuntimeException ignored) { }
                }
                if (m.policyClass() != PolicyClass.SAFE) want = false;
                m.setEnabled(want);
            }
            global.activeProfile = name;
        } finally {
            loading = false;
            dirty = false;
        }
    }

    public void saveProfile(String name) {
        if (!isValidProfileName(name)) return;
        JsonObject rootJson = new JsonObject();
        rootJson.addProperty("schema", SCHEMA_VERSION);
        JsonObject mods = new JsonObject();
        for (Module m : Nimbus.modules().all()) {
            JsonObject mj = new JsonObject();
            if (m.policyClass() == PolicyClass.SAFE) mj.addProperty("enabled", m.isEnabled());
            mj.add("settings", m.settingsToJson());
            mods.add(m.id(), mj);
        }
        rootJson.add("modules", mods);
        writeAtomic(profileFile(name), GSON.toJson(rootJson));
        dirty = false;
    }

    public List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        if (Files.isDirectory(profilesDir)) {
            try (Stream<Path> s = Files.list(profilesDir)) {
                s.map(p -> p.getFileName().toString())
                 .filter(f -> f.endsWith(".json"))
                 .map(f -> f.substring(0, f.length() - 5))
                 .filter(ConfigManager::isValidProfileName)
                 .sorted().forEach(names::add);
            } catch (IOException e) {
                Nimbus.LOG.warn("Could not list profiles: {}", e.toString());
            }
        }
        if (!names.contains("default")) names.add(0, "default");
        return names;
    }

    /** Saves the current state under a new name and makes it active. */
    public boolean createProfile(String name) {
        if (!isValidProfileName(name)) return false;
        saveProfile(name);
        global.activeProfile = name;
        saveGlobal();
        return true;
    }

    public boolean switchProfile(String name) {
        if (!isValidProfileName(name)) return false;
        saveProfile(global.activeProfile);
        loadProfile(name);
        saveGlobal();
        return true;
    }

    public boolean deleteProfile(String name) {
        if (!isValidProfileName(name) || name.equals("default") || name.equals(global.activeProfile)) return false;
        try { return Files.deleteIfExists(profileFile(name)); }
        catch (IOException e) { Nimbus.LOG.warn("Could not delete profile {}: {}", name, e.toString()); return false; }
    }

    // ---------------- autosave ----------------
    public void markDirty() {
        if (loading) return;
        dirty = true;
        dirtyTicks = 0;
    }

    public void tick() {
        if (dirty && ++dirtyTicks >= AUTOSAVE_DELAY_TICKS) saveProfile(global.activeProfile);
    }

    public void shutdown() {
        if (dirty) saveProfile(global.activeProfile);
        saveGlobal();
    }

    // ---------------- schema ----------------
    private JsonObject migrate(JsonObject json) {
        int version = 0;
        try { if (json.has("schema")) version = json.get("schema").getAsInt(); } catch (RuntimeException ignored) { }
        if (version > SCHEMA_VERSION) {
            Nimbus.LOG.warn("Profile was written by a newer Nimbus (schema {}); loading best-effort.", version);
            return json;
        }
        // Add `if (version < N) { ...transform json...; }` blocks here when the schema changes.
        json.addProperty("schema", SCHEMA_VERSION);
        return json;
    }

    // ---------------- io ----------------
    private Optional<JsonObject> readJson(Path file) {
        if (!Files.isRegularFile(file)) return Optional.empty();
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Nimbus.LOG.warn("Could not read {}: {}", file, e.toString());
            return Optional.empty();
        }
        try {
            JsonElement el = JsonParser.parseString(text);
            if (el.isJsonObject()) return Optional.of(el.getAsJsonObject());
            throw new JsonParseException("root is not an object");
        } catch (JsonParseException e) {
            Nimbus.LOG.warn("Corrupt config {} ({}); using defaults.", file.getFileName(), e.getMessage());
            backupCorrupt(file);
            return Optional.empty();
        }
    }

    private void backupCorrupt(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Nimbus.LOG.warn("Could not back up corrupt file {}: {}", file, e.toString());
        }
    }

    private void writeAtomic(Path target, String content) {
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Nimbus.LOG.error("Failed to save {}: {}", target, e.toString());
        }
    }
}
