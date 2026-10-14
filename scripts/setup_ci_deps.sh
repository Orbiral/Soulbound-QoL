#!/usr/bin/env bash
# ==============================================================================
# Soulbound QoL - CI Dependency Setup Script
# Downloads and prepares compile-time libraries on headless CI environments
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CI_LIBS="$SCRIPT_DIR/.ci-libs"
mkdir -p "$CI_LIBS"

echo "=== Preparing CI Compile Dependencies ==="

# 1. Download Mojang 1.21.1 Client & Intermediary Mappings if not present
INTER_JAR="$CI_LIBS/client-intermediary.jar"
if [[ ! -f "$INTER_JAR" ]]; then
    echo "Generating client-intermediary.jar for Minecraft 1.21.1..."
    TMP_DIR="$(mktemp -d)"
    
    # Tiny Remapper
    curl -sSL "https://maven.fabricmc.net/net/fabricmc/tiny-remapper/0.10.3/tiny-remapper-0.10.3-fat.jar" -o "$TMP_DIR/remapper.jar"
    
    # Intermediary Mappings
    curl -sSL "https://maven.fabricmc.net/net/fabricmc/intermediary/1.21.1/intermediary-1.21.1-v2.jar" -o "$TMP_DIR/inter.jar"
    unzip -q -o "$TMP_DIR/inter.jar" "mappings/mappings.tiny" -d "$TMP_DIR"
    
    # Mojang 1.21.1 Client Jar
    curl -sSL "https://piston-data.mojang.com/v1/objects/30c73b1c5da787909b2f73340419fdf13b9def88/client.jar" -o "$TMP_DIR/client.jar"
    
    # Remap
    java -jar "$TMP_DIR/remapper.jar" "$TMP_DIR/client.jar" "$INTER_JAR" "$TMP_DIR/mappings/mappings.tiny" official intermediary
    rm -rf "$TMP_DIR"
    echo "client-intermediary.jar generated successfully."
fi

# 2. Download Base Mod and Core Libraries
download_if_missing() {
    local target="$CI_LIBS/$1"
    local url="$2"
    if [[ ! -f "$target" ]]; then
        echo "Downloading $1..."
        curl -sSL -H "User-Agent: SoulboundQoL-CI/1.0" "$url" -o "$target"
    fi
}

download_if_missing "turnbattle-2.0.7.jar" "https://cdn.modrinth.com/data/xpMUaXfK/versions/9CCBEj0Z/turnbattle-2.0.7.jar"
download_if_missing "fabric-loader-0.16.10.jar" "https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.16.10/fabric-loader-0.16.10.jar"
download_if_missing "sponge-mixin-0.15.3.jar" "https://maven.fabricmc.net/net/fabricmc/sponge-mixin/0.15.3+mixin.0.8.7/sponge-mixin-0.15.3+mixin.0.8.7.jar"

# Mojang Libraries
download_if_missing "fastutil-8.5.12.jar" "https://libraries.minecraft.net/it/unimi/dsi/fastutil/8.5.12/fastutil-8.5.12.jar"
download_if_missing "joml-1.10.5.jar" "https://libraries.minecraft.net/org/joml/joml/1.10.5/joml-1.10.5.jar"
download_if_missing "gson-2.10.1.jar" "https://libraries.minecraft.net/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
download_if_missing "brigadier-1.2.9.jar" "https://libraries.minecraft.net/com/mojang/brigadier/1.2.9/brigadier-1.2.9.jar"
download_if_missing "netty-buffer-4.1.97.Final.jar" "https://libraries.minecraft.net/io/netty/netty-buffer/4.1.97.Final/netty-buffer-4.1.97.Final.jar"
download_if_missing "netty-common-4.1.97.Final.jar" "https://libraries.minecraft.net/io/netty/netty-common/4.1.97.Final/netty-common-4.1.97.Final.jar"
download_if_missing "slf4j-api-2.0.9.jar" "https://libraries.minecraft.net/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"

# Fabric API (Fat jar unpacked for sub-modules)
if [[ ! -f "$CI_LIBS/fabric-networking-api-v1.jar" ]]; then
    echo "Downloading and unpacking Fabric API modules..."
    FABRIC_API_JAR="$CI_LIBS/fabric-api.jar"
    download_if_missing "fabric-api.jar" "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.116.17+1.21.1/fabric-api-0.116.17+1.21.1.jar"
    unzip -q -j -o "$FABRIC_API_JAR" "META-INF/jars/*.jar" -d "$CI_LIBS/"
fi

echo "All CI compile dependencies ready in $CI_LIBS."
