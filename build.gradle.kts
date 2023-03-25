plugins {
    kotlin("jvm") version "1.7.20"
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

group = "top.colter.skiko"
version = "0.0.1"

mavenCentralPublish {
    useCentralS01()
    singleDevGithubProject("Colter23", "skiko-layout")
    licenseFromGitHubProject("MIT")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    val skikoVersion = "0.7.54"

    api("org.jetbrains.skiko:skiko-awt:$skikoVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")

    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:$skikoVersion")
//    testImplementation("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:$skikoVersion")
//    testImplementation("org.jetbrains.skiko:skiko-awt-runtime-linux-arm64:$skikoVersion")
//    testImplementation("org.jetbrains.skiko:skiko-awt-runtime-macos-x64:$skikoVersion")
//    testImplementation("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:$skikoVersion")
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
