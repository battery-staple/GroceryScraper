plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
    id("com.gradleup.shadow") version "9.4.2"
    id("com.github.node-gradle.node") version "7.1.0"
}

group = "com.groceryscraper"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.microsoft.playwright:playwright:1.49.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    
    val ktorVersion = "3.0.0"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.0")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
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
    useJUnitPlatform()
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

val installFrontendDependencies by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
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
