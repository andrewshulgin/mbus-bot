plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "com.andrewshulgin"
version = "3.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://releases.aspose.cloud/java/repo/") }
}

dependencies {
    implementation("com.aspose:aspose-barcode-cloud:22.7.0")
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.google.zxing:javase:3.5.2")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.1.0")
    implementation("io.sentry:sentry:6.27.0")
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("org.openmuc:jmbus:3.3.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "MainKt"
    val dependencies = configurations.runtimeClasspath.get().map(::zipTree)
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
