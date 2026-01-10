plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.cythonfix"
version = "1.0-SNAPSHOT"

// Use local PyCharm for faster local dev, download for CI
val useLocalPycharm = file("/home/sarandi/.local/share/JetBrains/Toolbox/apps/pycharm-professional").exists()
        && System.getenv("CI") == null

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        if (useLocalPycharm) {
            local("/home/sarandi/.local/share/JetBrains/Toolbox/apps/pycharm-professional")
        } else {
            pycharm("2025.3")
        }
        bundledPlugin("PythonCore")
        bundledPlugin("Pythonid")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        // Also make bundled plugins available in tests
        testBundledPlugin("PythonCore")
        testBundledPlugin("Pythonid")
    }
    testImplementation("junit:junit:4.13.2")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    patchPluginXml {
        sinceBuild.set("253")
        untilBuild.set("253.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
