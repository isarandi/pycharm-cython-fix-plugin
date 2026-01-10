# Plugin Packaging and Distribution

This document covers options for packaging, releasing, and distributing the Cython-Fix.

## Table of Contents

1. [Distribution Options Overview](#distribution-options-overview)
2. [Option 1: GitHub Releases (Recommended for Personal Use)](#option-1-github-releases)
3. [Option 2: JetBrains Marketplace (Public Distribution)](#option-2-jetbrains-marketplace)
4. [CI/CD with GitHub Actions](#cicd-with-github-actions)
5. [Plugin Signing](#plugin-signing)
6. [Manual Installation Guide](#manual-installation-guide)
7. [Local Development Setup](#local-development-setup)

---

## Distribution Options Overview

| Option                 | Effort      | Audience      | Review Process            | Auto-Updates |
| ---------------------- | ----------- | ------------- | ------------------------- | ------------ |
| GitHub Releases        | Low         | Personal/Team | None                      | No           |
| JetBrains Marketplace  | Medium-High | Public        | Manual review by JetBrains | Yes          |

---

## Option 1: GitHub Releases

**Best for:** Personal use, small teams, or distributing to specific users.

### Setup Steps

1. **Create a GitHub repository** for the plugin

2. **Build the plugin locally:**
   ```bash
   ./gradlew buildPlugin
   ```
   This creates a ZIP file in `build/distributions/`

3. **Create a GitHub Release:**
   - Go to your repo → Releases → "Create a new release"
   - Tag with version (e.g., `v1.0.0`)
   - Upload the ZIP file from `build/distributions/`
   - Add release notes describing changes

4. **Users download and install manually** (see [Manual Installation Guide](#manual-installation-guide))

### Pros
- No approval process
- Full control over releases
- Quick iteration

### Cons
- No automatic updates for users
- Users must manually check for new versions
- Less discoverable

---

## Option 2: JetBrains Marketplace

**Best for:** Public distribution with automatic updates.

### Requirements

Based on [JetBrains Marketplace Documentation](https://plugins.jetbrains.com/docs/marketplace/publishing-and-listing-your-plugin.html):

#### Plugin Requirements
- **Signed plugin** (required since 2021.2)
- **Maximum file size:** 400 MB
- **License:** Must provide EULA
- **Unique logo** (not the default template logo)
- **Description:** First 40 characters must be English summary

#### plugin.xml Requirements
```xml
<idea-plugin>
    <id>com.cythonfix.Cython-Fix</id>
    <name>Cython Fix</name>
    <vendor email="your@email.com" url="https://github.com/you/repo">
        Your Name
    </vendor>
    <description><![CDATA[
        Fixes Cython formatting and parsing issues in PyCharm.
        <!-- More detailed description -->
    ]]></description>
    <change-notes><![CDATA[
        <ul>
            <li>1.0.0 - Initial release</li>
        </ul>
    ]]></change-notes>
</idea-plugin>
```

#### Naming Guidelines
- Avoid "Support" or "Integration" in names
- No trademarked names without authorization
- No pricing information in name

### Publishing Process

1. **Create JetBrains Account** at [hub.jetbrains.com](https://hub.jetbrains.com)

2. **Get Marketplace Token:**
   - Go to [Marketplace](https://plugins.jetbrains.com) → Profile → My Tokens
   - Generate a new token

3. **First Upload (Manual):**
   - Build and sign the plugin
   - Upload at [plugins.jetbrains.com/plugin/add](https://plugins.jetbrains.com/plugin/add)
   - Wait for review (3-4 business days typical)

4. **Subsequent Updates:**
   - Can be automated via `publishPlugin` Gradle task
   - Still subject to review

### Review Process

Per [Approval Guidelines](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html):
- All plugins and updates are manually reviewed
- JetBrains checks for security, quality, and guideline compliance
- No guaranteed timeframe
- Contact marketplace@jetbrains.com if no response in 3-4 days

---

## CI/CD with GitHub Actions

Based on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).

### Recommended Workflow Structure

Create `.github/workflows/build.yml`:

```yaml
name: Build

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: zulu
          java-version: 21

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build Plugin
        run: ./gradlew buildPlugin

      - name: Verify Plugin
        run: ./gradlew verifyPlugin

      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: plugin-artifact
          path: build/distributions/*.zip

  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: zulu
          java-version: 21

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run Tests
        run: ./gradlew check

  # Optional: Run IntelliJ Plugin Verifier
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: zulu
          java-version: 21

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run Plugin Verifier
        run: ./gradlew runPluginVerifier

      - name: Upload Verifier Results
        uses: actions/upload-artifact@v4
        with:
          name: verifier-results
          path: build/reports/pluginVerifier
```

### Release Workflow

Create `.github/workflows/release.yml`:

```yaml
name: Release

on:
  release:
    types: [published]

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: zulu
          java-version: 21

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build Plugin
        run: ./gradlew buildPlugin

      - name: Sign Plugin
        run: ./gradlew signPlugin
        env:
          CERTIFICATE_CHAIN: ${{ secrets.CERTIFICATE_CHAIN }}
          PRIVATE_KEY: ${{ secrets.PRIVATE_KEY }}
          PRIVATE_KEY_PASSWORD: ${{ secrets.PRIVATE_KEY_PASSWORD }}

      - name: Upload to GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          files: build/distributions/*.zip

      # Optional: Publish to Marketplace
      - name: Publish to Marketplace
        run: ./gradlew publishPlugin
        env:
          PUBLISH_TOKEN: ${{ secrets.PUBLISH_TOKEN }}
```

### Required GitHub Secrets

Configure in Settings → Secrets and variables → Actions:

| Secret                   | Description                                              |
| ------------------------ | -------------------------------------------------------- |
| `CERTIFICATE_CHAIN`      | Contents of `chain.crt` file                             |
| `PRIVATE_KEY`            | Contents of `private.pem` file                           |
| `PRIVATE_KEY_PASSWORD`   | Password for private key (if encrypted)                  |
| `PUBLISH_TOKEN`          | JetBrains Marketplace token (for Marketplace publishing) |

---

## Plugin Signing

Required for JetBrains Marketplace and recommended for all distributions.

Based on [Plugin Signing Documentation](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html).

### Generate Signing Certificates

```bash
# Generate encrypted private key (4096-bit RSA)
openssl genpkey -aes-256-cbc -algorithm RSA \
    -out private_encrypted.pem -pkeyopt rsa_keygen_bits:4096

# Convert to standard RSA format
openssl rsa -in private_encrypted.pem -out private.pem

# Generate self-signed certificate (valid 1 year)
openssl req -key private.pem -new -x509 -days 365 -out chain.crt
```

### Configure build.gradle.kts

Already configured in this project:

```kotlin
signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
}

publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
}
```

### Sign Locally

```bash
export CERTIFICATE_CHAIN=$(cat chain.crt)
export PRIVATE_KEY=$(cat private.pem)
export PRIVATE_KEY_PASSWORD="your-password"

./gradlew signPlugin
```

The signed ZIP will be in `build/distributions/`.

---

## Manual Installation Guide

For users installing from GitHub Releases or local builds:

1. **Download** the plugin ZIP file (do NOT unzip)

2. **Open PyCharm** → Settings/Preferences (`Ctrl+Alt+S` / `Cmd+,`)

3. **Navigate to Plugins**

4. **Click the gear icon (⚙️)** → "Install Plugin from Disk..."

5. **Select the ZIP file** and click OK

6. **Restart PyCharm** when prompted

### Compatibility Note

Ensure the plugin version matches your PyCharm version. Check `sinceBuild` and `untilBuild` in `build.gradle.kts`:

```kotlin
patchPluginXml {
    sinceBuild.set("253")      // Minimum: 2025.3
    untilBuild.set("253.*")    // Maximum: 2025.3.x
}
```

---

## Local Development Setup

### JDK Requirement

Building requires JDK 21+. Options:

1. **Install OpenJDK:**
   ```bash
   sudo apt install openjdk-21-jdk
   ```

2. **Or use PyCharm's bundled JBR** (if no JDK installed):
   Create `gradle.properties` (not committed to git):
   ```properties
   org.gradle.java.home=/path/to/JetBrains/Toolbox/apps/pycharm-professional/jbr
   ```

### Building Locally

```bash
# Build the plugin
./gradlew buildPlugin

# Run tests
./gradlew test

# Run IDE with plugin for manual testing
./gradlew runIde
```

**Note:** The formatter test skips automatically if Cython language support isn't available in the test environment. Full testing requires PyCharm Professional with Django module enabled.

---

## Quick Start: GitHub Release Distribution

For the simplest distribution path:

```bash
# 1. Build the plugin
./gradlew buildPlugin

# 2. The ZIP is ready at:
ls build/distributions/
# Cython-Fix-1.0-SNAPSHOT.zip

# 3. Create GitHub release and upload the ZIP
# 4. Share the release URL with users
```

---

## References

- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
- [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
- [JetBrains Marketplace Guidelines](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html)
- [IntelliJ Platform Gradle Plugin](https://github.com/JetBrains/intellij-platform-gradle-plugin)
