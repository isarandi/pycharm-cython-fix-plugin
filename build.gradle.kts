plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.cythonfix"
version = "0.2.0"

// Use local PyCharm for faster local dev, download for CI
val pycharmLocalPath = providers.gradleProperty("pycharmLocalPath").orNull
val useLocalPycharm = pycharmLocalPath != null && file(pycharmLocalPath).exists()
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
            local(pycharmLocalPath!!)
        } else {
            pycharm("2025.3")
        }
        bundledPlugin("PythonCore")
        bundledPlugin("Pythonid")
    }
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
