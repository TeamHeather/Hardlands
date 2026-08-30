plugins {
    id("java-library")
    kotlin("jvm")

    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.chunky.common)
    implementation(libs.acf.paper)

    compileOnly("com.google.code.gson:gson:2.14.0")

    compileOnly(project(":annotation-processor"))
    annotationProcessor(project(":annotation-processor"))
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    jar {
        archiveClassifier.set("unshaded")
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("co.aikar.commands", "team.heather.hardlands.libs.acf.commands")
        relocate("co.aikar.locales", "team.heather.hardlands.libs.acf.locales")
    }

    assemble {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms4G", "-Xmx4G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)

        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}