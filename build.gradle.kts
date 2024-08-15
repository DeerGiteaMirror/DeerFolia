import io.papermc.paperweight.util.*

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("io.papermc.paperweight.patcher") version "1.7.1"
}

allprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

repositories {
    mavenCentral()
    mavenLocal()
    maven(paperMavenPublicUrl) {
        content {
            onlyForConfigurations(configurations.paperclip.name)
        }
    }
}

dependencies {
    remapper("net.fabricmc:tiny-remapper:0.10.3:fat")
    decompiler("org.vineflower:vineflower:1.10.1")
    paperclip("io.papermc:paperclip:3.0.3")
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://ci.emc.gs/nexus/content/groups/aikar/")
        maven("https://repo.aikar.co/content/groups/aikar")
        maven("https://repo.md-5.net/content/repositories/releases/")
        maven("https://hub.spigotmc.org/nexus/content/groups/public/")
        maven("https://jitpack.io")
        maven("https://repo.codemc.io/repository/maven-public/")
    }
}

paperweight {
    serverProject.set(project(":deer-folia-server"))

    remapRepo.set(paperMavenPublicUrl)
    decompileRepo.set(paperMavenPublicUrl)

    useStandardUpstream("Folia") {
        url.set("https://ssl.lunadeer.cn:14446/mirror/Folia.git")
        ref.set(providers.gradleProperty("foliaRef"))

        withStandardPatcher {
            baseName("Folia")

            apiPatchDir.set(layout.projectDirectory.dir("patches/api"))
            apiOutputDir.set(layout.projectDirectory.dir("deer-folia-api"))

            serverPatchDir.set(layout.projectDirectory.dir("patches/server"))
            serverOutputDir.set(layout.projectDirectory.dir("deer-folia-server"))
        }
        patchTasks.register("generatedApi") {
            isBareDirectory = true
            upstreamDirPath = "paper-api-generator/generated"
            patchDir = layout.projectDirectory.dir("patches/generatedApi")
            outputDir = layout.projectDirectory.dir("paper-api-generator/generated")
        }
    }
}

tasks.register("updateFoliaRef") {
    // 更新 Folia 依赖的最新 commit
    val tempDir = layout.cacheDir("foliaRefLatest")
    val file = "gradle.properties"

    doFirst {
        val foliaLatestCommitJson = layout.cache.resolve("foliaLatestCommit.json")
        download.get().download("https://ssl.lunadeer.cn:14446/api/v1/repos/mirror/Folia/commits?sha=master", foliaLatestCommitJson)
        val foliaLatestCommit = gson.fromJson<paper.libs.com.google.gson.JsonArray>(foliaLatestCommitJson).get(0).asJsonObject.get("sha").asString

        copy {
            from(file)
            into(tempDir)
            filter { line: String ->
                line.replace("foliaRef = .*".toRegex(), "foliaRef = $foliaLatestCommit")
            }
        }
    }

    doLast {
        copy {
            from(tempDir.file("gradle.properties"))
            into(project.file(file).parent)
        }
    }
}