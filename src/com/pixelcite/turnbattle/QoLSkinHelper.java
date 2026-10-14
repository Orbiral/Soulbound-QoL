package com.pixelcite.turnbattle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_2960;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public class QoLSkinHelper {

    private static final Map<String, class_2960> DARK_CACHE = new ConcurrentHashMap<>();

    public static Path skinDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("soulbound-recruit-skins");
    }

    /**
     * Sanitizes a string for cross-platform filesystem use (Windows, macOS, Linux).
     * Strips illegal filesystem characters and guards against Windows reserved device names.
     */
    public static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "recruit";
        }
        String cleaned = name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_").trim();
        cleaned = cleaned.replaceAll("[. ]+$", "");
        if (cleaned.isBlank()) {
            cleaned = "recruit";
        }
        // Protect against Windows reserved device names (CON, PRN, AUX, NUL, COM1-9, LPT1-9)
        if (cleaned.toUpperCase(Locale.ROOT).matches("^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$")) {
            cleaned = "_" + cleaned;
        }
        return cleaned;
    }

    /**
     * Sanitizes a string for Minecraft Identifier paths (strictly [a-z0-9_.-]).
     * Prevents InvalidIdentifierException on non-standard recruit names or OS paths.
     */
    public static String sanitizeIdentifierPath(String input) {
        if (input == null || input.isBlank()) {
            return "recruit";
        }
        String sanitized = input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        if (sanitized.isBlank()) {
            return "recruit";
        }
        return sanitized;
    }

    /**
     * Cross-platform path resolver for user configuration.
     * Expands home directory (~ on macOS/Linux) and environment variables (%VAR% on Windows).
     */
    public static Path resolveCustomPath(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String path = raw.trim();
        if (path.startsWith("~/") || path.startsWith("~\\")) {
            String userHome = System.getProperty("user.home");
            path = userHome + File.separator + path.substring(2);
        } else if (path.startsWith("%") && path.indexOf('%', 1) > 0) {
            int end = path.indexOf('%', 1);
            String envVar = path.substring(1, end);
            String envVal = System.getenv(envVar);
            if (envVal != null) {
                path = envVal + path.substring(end + 1);
            }
        }
        try {
            Path p = Path.of(path);
            return Files.exists(p) ? p : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, String> explicitPaths() {
        Path file = skinDir().resolve("recruit_skin_paths.json");
        if (!Files.exists(file)) return null;
        try {
            String json = Files.readString(file);
            Map<String, String> result = new ConcurrentHashMap<>();
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            for (String key : obj.keySet()) {
                result.put(key, obj.get(key).getAsString());
            }
            return result;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean isDark() {
        if (DarkWorldSkinsClient.force == 1) {
            return false;
        }
        if (DarkWorldSkinsClient.force == 2) {
            return true;
        }
        if (!DarkWorldSkinsClient.inDarkWorld()) {
            return false;
        }
        return !DarkWorldFall.hidingNewSkin();
    }

    public static boolean hasDarkSkin(String recruitName) {
        if (recruitName == null || recruitName.isBlank()) {
            return false;
        }
        String key = recruitName.toLowerCase(Locale.ROOT).trim();
        Map<String, String> exp = explicitPaths();
        if (exp != null && exp.containsKey(key + "_dark")) {
            Path p = resolveCustomPath(exp.get(key + "_dark"));
            if (p != null) return true;
        }
        Path dir = skinDir();
        String safe = sanitizeFileName(key);
        return findSkinFile(dir, safe + "_dark", safe + "-dark", safe + "_darkworld", safe + "_dw") != null;
    }

    /**
     * Cross-platform skin file finder.
     * Uses fast direct lookup, falling back to a case-insensitive directory scan
     * to guarantee identical forgiving behavior across Windows, macOS, and Linux.
     */
    public static Path findSkinFile(Path dir, String... candidates) {
        if (dir == null || !Files.isDirectory(dir)) return null;
        // 1. Direct path check (fastest on all platforms)
        for (String c : candidates) {
            Path p = dir.resolve(c + ".png");
            if (Files.exists(p)) return p;
        }
        // 2. Case-insensitive scan fallback (guarantees Linux matches Windows/macOS tolerance)
        try (var stream = Files.list(dir)) {
            List<Path> files = stream.filter(Files::isRegularFile).toList();
            for (String c : candidates) {
                String expected = c + ".png";
                for (Path f : files) {
                    if (f.getFileName().toString().equalsIgnoreCase(expected)) {
                        return f;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void makeBaseOpaque(class_1011 img) {
        if (img == null) return;
        int w = img.method_4307();
        int h = img.method_4323();
        for (int y = 0; y < Math.min(32, h); ++y) {
            for (int x = 0; x < w; ++x) {
                if (y < 16 && (x >= 32 && x < 64)) continue;
                int col = img.method_4315(x, y);
                img.method_4305(x, y, col | 0xFF000000);
            }
        }
    }

    public static class_2960 loadCustomSkin(Path file, String cacheKey) {
        if (file == null || !Files.exists(file)) return null;
        try (InputStream is = Files.newInputStream(file)) {
            class_1011 img = class_1011.method_4309(is);
            makeBaseOpaque(img);
            class_1043 tex = new class_1043(img);
            class_2960 id = class_2960.method_60655("turnbattle", "recruit_skin/" + sanitizeIdentifierPath(cacheKey));
            class_310.method_1551().method_1531().method_4616(id, (class_1044)tex);
            return id;
        } catch (Throwable t) {
            TurnBattleMod.LOGGER.warn("[Soulbound QoL] Failed to load skin '{}': {}", file, t.getMessage());
            return null;
        }
    }

    public static class_2960 getCustomSkin(String recruitName) {
        if (recruitName == null || recruitName.isBlank()) {
            return null;
        }
        String key = recruitName.toLowerCase(Locale.ROOT).trim();
        boolean dark = isDark();
        String cacheKey = (dark ? "dark:" : "light:") + key;

        class_2960 cached = DARK_CACHE.get(cacheKey);
        if (cached != null) return cached;

        Path dir = skinDir();
        String safe = sanitizeFileName(key);
        Map<String, String> exp = explicitPaths();

        if (dark) {
            if (exp != null) {
                String explicitDark = exp.get(key + "_dark");
                if (explicitDark != null) {
                    Path p = resolveCustomPath(explicitDark);
                    if (p != null) {
                        class_2960 res = loadCustomSkin(p, cacheKey);
                        if (res != null) {
                            DARK_CACHE.put(cacheKey, res);
                            return res;
                        }
                    }
                }
            }
            Path darkFile = findSkinFile(dir, safe + "_dark", safe + "-dark", safe + "_darkworld", safe + "_dw");
            if (darkFile != null) {
                class_2960 res = loadCustomSkin(darkFile, cacheKey);
                if (res != null) {
                    DARK_CACHE.put(cacheKey, res);
                    return res;
                }
            }
        }

        if (exp != null) {
            String explicit = exp.get(key);
            if (explicit != null) {
                Path p = resolveCustomPath(explicit);
                if (p != null) {
                    class_2960 res = loadCustomSkin(p, cacheKey);
                    if (res != null) {
                        DARK_CACHE.put(cacheKey, res);
                        return res;
                    }
                }
            }
        }
        Path normalFile = findSkinFile(dir, safe);
        if (normalFile != null) {
            class_2960 res = loadCustomSkin(normalFile, cacheKey);
            if (res != null) {
                DARK_CACHE.put(cacheKey, res);
                return res;
            }
        }

        return null;
    }
}
