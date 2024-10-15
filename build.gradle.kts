plugins {
    kotlin("jvm") version "1.9.0"
    id("maven-publish")
}

group = "top.colter.skiko"
version = "0.0.2"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "top.colter.skiko"
            artifactId = "skiko-layout"
            version = "0.0.2"

            from(components["kotlin"])
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    val skikoVersion = "0.7.71"

    api("org.jetbrains.skiko:skiko-awt:$skikoVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")

    implementation(kotlin("reflect"))

    val osName = System.getProperty("os.name")
    val targetOs = when {
        osName == "Mac OS X" -> "macos"
        osName.startsWith("Win") -> "windows"
        osName.startsWith("Linux") -> "linux"
        else -> error("Unsupported OS: $osName")
    }

    val osArch = System.getProperty("os.arch")
    val targetArch = when (osArch) {
        "x86_64", "amd64" -> "x64"
        "aarch64" -> "arm64"
        else -> error("Unsupported arch: $osArch")
    }
    val target = "${targetOs}-${targetArch}"
    testImplementation("org.jetbrains.skiko:skiko-awt-runtime-$target:$skikoVersion")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    explicitApi()
    target.compilations.all {
        kotlinOptions.jvmTarget = "11"
    }
}
