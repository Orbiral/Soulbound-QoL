#!/usr/bin/env bash
# ==============================================================================
# Soulbound QoL - Build Script
# Compiles all sources with Java 21 and packages soulbound-qol-1.0.0.jar
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Building Soulbound QoL Addon ==="

# 1. Locate Java 21 compiler
JAVAC_BIN="${JAVA_HOME:-}/bin/javac"
if [[ ! -x "$JAVAC_BIN" ]]; then
    JAVAC_BIN="$(which javac || true)"
fi

if [[ -z "$JAVAC_BIN" ]]; then
    echo "Error: javac not found! Please set JAVA_HOME or ensure javac is in PATH." >&2
    exit 1
fi

JAVA_VER="$("$JAVAC_BIN" -version 2>&1 | awk '{print $2}')"
echo "Using javac: $JAVAC_BIN (version $JAVA_VER)"

# 2. Configure Classpath
# Check common profile and meta locations (Linux Flatpak, Linux standard, macOS)
MODRINTH_PROFILE=""
for candidate in \
    "${HOME}/.var/app/com.modrinth.ModrinthApp/data/ModrinthApp/profiles/Soulbound" \
    "${HOME}/.local/share/ModrinthApp/profiles/Soulbound" \
    "${HOME}/Library/Application Support/ModrinthApp/profiles/Soulbound" \
    "${HOME}/.minecraft"; do
    if [[ -d "$candidate" ]]; then
        MODRINTH_PROFILE="$candidate"
        break
    fi
done

FABRIC_META=""
for candidate in \
    "${HOME}/.var/app/com.modrinth.ModrinthApp/data/ModrinthApp/meta" \
    "${HOME}/.local/share/ModrinthApp/meta" \
    "${HOME}/Library/Application Support/ModrinthApp/meta" \
    "${HOME}/.minecraft/libraries"; do
    if [[ -d "$candidate" ]]; then
        FABRIC_META="$candidate"
        break
    fi
done

# Intermediary Minecraft Jar
MC_JAR=""
if [[ -f "$SCRIPT_DIR/.ci-libs/client-intermediary.jar" ]]; then
    MC_JAR="$SCRIPT_DIR/.ci-libs/client-intermediary.jar"
elif [[ -n "$MODRINTH_PROFILE" && -d "$MODRINTH_PROFILE/.fabric/remappedJars" ]]; then
    MC_JAR="$(find "$MODRINTH_PROFILE/.fabric/remappedJars" -name "client-intermediary.jar" 2>/dev/null | head -n 1 || true)"
fi
if [[ -z "$MC_JAR" || ! -f "$MC_JAR" ]]; then
    MC_JAR="$(find "${HOME}/.local" "${HOME}/.var" -name "client-intermediary.jar" 2>/dev/null | head -n 1 || true)"
fi

# Upstream TurnBattle Mod Jar
UPSTREAM_JAR="$SCRIPT_DIR/libs/turnbattle-2.0.7.jar"
if [[ ! -f "$UPSTREAM_JAR" && -f "$SCRIPT_DIR/.ci-libs/turnbattle-2.0.7.jar" ]]; then
    UPSTREAM_JAR="$SCRIPT_DIR/.ci-libs/turnbattle-2.0.7.jar"
fi
if [[ ! -f "$UPSTREAM_JAR" ]]; then
    UPSTREAM_JAR="/tmp/upstream_patches/turnbattle-2.0.7.jar"
fi
if [[ ! -f "$UPSTREAM_JAR" && -n "$MODRINTH_PROFILE" ]]; then
    UPSTREAM_JAR="$(find "$MODRINTH_PROFILE/mods" -name "turnbattle*.jar" 2>/dev/null | head -n 1 || true)"
fi

# Fabric Loader & Sponge Mixin
FL_JAR="$(find "$FABRIC_META/libraries/net/fabricmc/fabric-loader" -name "fabric-loader-*.jar" 2>/dev/null | head -n 1 || true)"
MX_JAR="$(find "$FABRIC_META/libraries/net/fabricmc/sponge-mixin" -name "sponge-mixin-*.jar" 2>/dev/null | head -n 1 || true)"
SLF_JAR="$(find "$FABRIC_META/libraries/org/slf4j/slf4j-api" -name "slf4j-api-*.jar" 2>/dev/null | head -n 1 || true)"
FU_JAR="$(find "$FABRIC_META/libraries/it/unimi/dsi/fastutil" -name "fastutil-*.jar" 2>/dev/null | head -n 1 || true)"
JM_JAR="$(find "$FABRIC_META/libraries/org/joml/joml" -name "joml-*.jar" 2>/dev/null | head -n 1 || true)"
GS_JAR="$(find "$FABRIC_META/libraries/com/google/code/gson/gson" -name "gson-*.jar" 2>/dev/null | head -n 1 || true)"
BR_JAR="$(find "$FABRIC_META/libraries/com/mojang/brigadier" -name "brigadier-*.jar" 2>/dev/null | head -n 1 || true)"
NB_JAR="$(find "$FABRIC_META/libraries/io/netty/netty-buffer" -name "netty-buffer-*.jar" 2>/dev/null | head -n 1 || head -n 1 || true)"
NC_JAR="$(find "$FABRIC_META/libraries/io/netty/netty-common" -name "netty-common-*.jar" 2>/dev/null | head -n 1 || head -n 1 || true)"
FNET_JAR="$(find "$MODRINTH_PROFILE/.fabric/processedMods" -name "fabric-networking-api-v1-*.jar" 2>/dev/null | head -n 1 || true)"
FLIF_JAR="$(find "$MODRINTH_PROFILE/.fabric/processedMods" -name "fabric-lifecycle-events-v1-*.jar" 2>/dev/null | head -n 1 || true)"
FAPI_JAR="$(find "$MODRINTH_PROFILE/.fabric/processedMods" -name "fabric-api-base-*.jar" 2>/dev/null | head -n 1 || true)"

CP="$MC_JAR:$UPSTREAM_JAR:$FL_JAR:$MX_JAR:$SLF_JAR:$FU_JAR:$JM_JAR:$GS_JAR:$BR_JAR:$NB_JAR:$NC_JAR:$FNET_JAR:$FLIF_JAR:$FAPI_JAR"

# Include CI libraries if present
if [[ -d "$SCRIPT_DIR/.ci-libs" ]]; then
    CI_CP="$(find "$SCRIPT_DIR/.ci-libs" -name "*.jar" 2>/dev/null | tr '\n' ':')"
    CP="${CP}:${CI_CP}"
fi

# 3. Clean and prepare build directory
rm -rf build/classes
mkdir -p build/classes build/libs

# 4. Compile sources
echo "Compiling Java sources..."
SOURCES=$(find src -name "*.java")
"$JAVAC_BIN" --release 21 -proc:none -cp "$CP" -d build/classes $SOURCES

# 5. Copy resources
echo "Copying mod resources..."
cp src/resources/fabric.mod.json build/classes/
cp src/resources/soulbound-qol.mixins.json build/classes/

# 6. Package JAR
echo "Packaging soulbound-qol-1.0.0.jar..."
jar -cf build/libs/soulbound-qol-1.0.0.jar -C build/classes .
cp build/libs/soulbound-qol-1.0.0.jar soulbound-qol-1.0.0.jar

echo "Build successful! Created: $SCRIPT_DIR/soulbound-qol-1.0.0.jar"

# 7. Optional install to profile
if [[ "${1:-}" == "--install" || "${1:-}" == "-i" ]]; then
    TARGET_DIR="$MODRINTH_PROFILE/mods"
    if [[ -d "$TARGET_DIR" ]]; then
        cp soulbound-qol-1.0.0.jar "$TARGET_DIR/soulbound-qol-1.0.0.jar"
        echo "Installed to $TARGET_DIR/soulbound-qol-1.0.0.jar"
    else
        echo "Warning: Target mods directory $TARGET_DIR does not exist."
    fi
fi
