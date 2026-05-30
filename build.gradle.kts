plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    application
    id("com.gradleup.shadow") version "9.4.2"
    id("com.github.node-gradle.node") version "7.1.0"
}

val gitVersionProvider = providers.exec {
    commandLine("git", "describe", "--tags", "--abbrev=0")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().removePrefix("v") }.orElse("1.0-SNAPSHOT")

group = "com.groceryscraper"
version = gitVersionProvider.get()

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.microsoft.playwright:playwright:1.60.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.slf4j:slf4j-simple:2.0.18")
    
    val ktorVersion = "3.5.0"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testImplementation("com.google.truth:truth:1.4.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

application {
    mainClass.set("AppKt")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.getByName<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.test {
    useJUnitPlatform {
        if (System.getenv("CI") != null) {
            excludeTags("flaky")
        }
    }
}

tasks.shadowJar {
    archiveBaseName.set("grocery-scraper")
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.register<JavaExec>("reflect") {
    mainClass.set("ReflectKt")
    classpath = sourceSets["main"].runtimeClasspath
}

kotlin {
    jvmToolchain(21)
}

val frontendDir = file("$projectDir/frontend")

node {
    download = true
    version = "22.14.0"
    npmVersion = "10.9.2"
    nodeProjectDir = frontendDir
}

val syncFrontendVersion by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    npmCommand = listOf("version", version.toString(), "--no-git-tag-version", "--allow-same-version")
}

val installFrontendDependencies by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    dependsOn(syncFrontendVersion)
    npmCommand = listOf("install")

    inputs.file(frontendDir.resolve("package.json"))
    inputs.file(frontendDir.resolve("package-lock.json"))
    outputs.dir(frontendDir.resolve("node_modules"))
}

val buildFrontend by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    dependsOn(installFrontendDependencies)
    npmCommand = listOf("run", "build")

    inputs.dir(frontendDir.resolve("src"))
    inputs.dir(frontendDir.resolve("public"))
    inputs.file(frontendDir.resolve("package.json"))
    inputs.file(frontendDir.resolve("package-lock.json"))
    inputs.file(frontendDir.resolve("vite.config.js"))
    inputs.file(frontendDir.resolve("index.html"))

    outputs.dir(frontendDir.resolve("dist"))
}

val processFrontendResources by tasks.registering(Copy::class) {
    dependsOn(buildFrontend)
    from(frontendDir.resolve("dist"))
    into(layout.buildDirectory.dir("generated/frontendResources/web"))
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.dir("generated/frontendResources"))
        }
    }
}

tasks.named("processResources") {
    dependsOn(processFrontendResources)
}

tasks.register("executable") {
    dependsOn(tasks.shadowJar)
    val shadowJarTask = tasks.shadowJar.get()
    
    inputs.file(shadowJarTask.archiveFile)
    
    val outputDir = layout.buildDirectory.dir("executable")
    val unixFile = outputDir.map { it.file("grocery-scraper") }
    val winFile = outputDir.map { it.file("grocery-scraper.bat") }
    outputs.file(unixFile)
    outputs.file(winFile)
    
    doLast {
        val originalFile = shadowJarTask.archiveFile.get().asFile
        val execUnix = unixFile.get().asFile
        val execWin = winFile.get().asFile
        execUnix.parentFile.mkdirs()
        
        // Generate Unix executable
        execUnix.outputStream().use { os ->
            os.write("#!/bin/sh\nexec java --enable-native-access=ALL-UNNAMED -jar \"\$0\" \"\$@\"\n".toByteArray())
            originalFile.inputStream().use { it.copyTo(os) }
        }
        execUnix.setExecutable(true)

        // Generate Windows executable
        execWin.outputStream().use { os ->
            os.write("@echo off\r\njava --enable-native-access=ALL-UNNAMED -jar \"%~f0\" %*\r\nexit /b %errorlevel%\r\n".toByteArray())
            originalFile.inputStream().use { it.copyTo(os) }
        }
    }
}
