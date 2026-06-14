import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.0"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "top.colter.skiko"
version = "0.0.5"

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    val skikoVersion = "0.148.1"

    compileOnly("org.jetbrains.skiko:skiko-awt:$skikoVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

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
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("top.colter.skiko", "skiko-layout", version.toString())

    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = SourcesJar.Sources(),
        )
    )

    pom {
        name.set("Skiko Layout")
        description.set("Convenient use of Skiko for static layout without calculating position and size.")
        url.set("https://github.com/Colter23/skiko-layout")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/Colter23/skiko-layout/blob/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("Colter23")
                name.set("Colter")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/Colter23/skiko-layout")
            url.set("https://github.com/Colter23/skiko-layout")
        }
    }
}
