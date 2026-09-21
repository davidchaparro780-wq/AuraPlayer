#!/usr/bin/env bash
set -e

# 1. If gradle command is already available, use it directly
if command -v gradle >/dev/null 2>&1; then
    GRADLE_VER=$(gradle -v | grep 'Gradle ' | awk '{print $2}')
    if [[ "$GRADLE_VER" == 8.* ]]; then
        echo "Using system Gradle $GRADLE_VER"
        exec gradle "$@"
    fi
fi

# 2. Resilient download and extract of Gradle 8.10.2 with retries and mirror fallback
GRADLE_HOME="/tmp/gradle/gradle-8.10.2"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
    echo "Downloading Gradle 8.10.2 with retry and fallback mirrors..."
    mkdir -p /tmp/gradle
    
    DOWNLOAD_SUCCESS=0
    for URL in \
        "https://mirrors.cloud.tencent.com/gradle/gradle-8.10.2-bin.zip" \
        "https://services.gradle.org/distributions/gradle-8.10.2-bin.zip" \
        "https://repo.gradle.org/artifactory/libs-releases/org/gradle/gradle/8.10.2/gradle-8.10.2-bin.zip"
    do
        echo "Trying: $URL"
        if curl -f -sSL --retry 3 --retry-delay 2 "$URL" -o /tmp/gradle/gradle-8.10.2-bin.zip; then
            DOWNLOAD_SUCCESS=1
            break
        fi
    done

    if [ "$DOWNLOAD_SUCCESS" -ne 1 ]; then
        echo "Failed to download Gradle 8.10.2 from all mirrors."
        exit 1
    fi

    echo "Extracting Gradle 8.10.2..."
    unzip -q -o /tmp/gradle/gradle-8.10.2-bin.zip -d /tmp/gradle
    chmod +x "$GRADLE_HOME/bin/gradle"
fi

echo "Executing Gradle 8.10.2..."
exec "$GRADLE_HOME/bin/gradle" "$@"
