import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.1"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "ru.nsu_null"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}
dependencies {
    // TODO test this on windows
    // https://mvnrepository.com/artifact/org.jetbrains.skiko/skiko-awt
    implementation("org.jetbrains.skiko:skiko-awt:0.7.22")

    implementation(compose.desktop.currentOs)
    implementation("org.antlr:antlr4:4.10.1")
    api(compose.materialIconsExtended)
    implementation("com.charleskorn.kaml:kaml:0.44.0")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.fifesoft:rsyntaxtextarea:3.2.0")
    implementation("me.tomassetti.kanvas:kanvas-core:0.2.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "ru.nsu_null.npide.MainKt"
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree) // OR .map { zipTree(it) }
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "NPidE"
            packageVersion = "1.0.0"
        }
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
