plugins {
    kotlin("jvm") version "2.0.21"
    id("maven-publish")
    id("signing")
}

group = "top.colter.skiko"
version = "0.0.3-BETA1"

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    val skikoVersion = "0.8.16"

    compileOnly("org.jetbrains.skiko:skiko-awt:$skikoVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

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
    withJavadocJar()
}

kotlin {
    explicitApi()
    target.compilations.all {
        kotlinOptions.jvmTarget = "11"
    }

}

val ver = version.toString()

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("kotlinSourcesJar"))
            from(components["kotlin"])

            groupId = "top.colter.skiko"
            artifactId = "skiko-layout"
            version = ver

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
    }

    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username=properties["ossrhUsername"].toString()
                password=properties["ossrhPassword"].toString()
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}