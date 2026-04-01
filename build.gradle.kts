import java.util.Properties

plugins {
    id("java")
    id("maven-publish")
    id("application")

    // 🛑 CORREÇÃO: Usando o ID e a versão CORRETOS conforme a documentação oficial.
    id("org.openjfx.javafxplugin") version "0.1.0"
}

val props = Properties()
file("gradle.properties").inputStream().use { props.load(it) }


group = "megalodonte"
version = props.getProperty("appVersion")

repositories {
    mavenCentral()
    mavenLocal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}


// 🛑 2. CONFIGURA O PLUGIN DO JAVAFX
javafx {
    version = "17" // Mantida a versão 17.0.10.

    modules("javafx.controls", "javafx.graphics", "javafx.web")
}

dependencies {
    // Dependências de teste (mantidas)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("megalodonte:megalodonte-base:1.0.0-beta")
    implementation("megalodonte:megalodonte-components:1.0.0-beta")
    implementation("megalodonte:megalodonte-reactivity:1.0.0-beta")
    implementation("megalodonte:megalodonte-router:1.0.0-beta")

    implementation("com.fasterxml.jackson.core:jackson-core:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")

    implementation("org.kordamp.ikonli:ikonli-core:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-javafx:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-antdesignicons-pack:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-entypo-pack:12.4.0")

    implementation("org.jsoup:jsoup:1.18.3")

    implementation("org.seleniumhq.selenium:selenium-java:4.18.1")
    implementation("io.github.bonigarcia:webdrivermanager:5.7.0")

    implementation("org.controlsfx:controlsfx:11.2.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set(props.getProperty("appMainClass"))
}

tasks.jar {
    enabled = true
    archiveBaseName.set(props.getProperty("appName"))

    manifest {
        attributes(
            "Implementation-Title" to "JavaFX ${props.getProperty("appName")} app",
            "Implementation-Version" to project.version,
            "Main-Class" to props.getProperty("appMainClass")
        )
    }
}

//no caso vai copiar os jar dinamicamente que a aplicação ta usando
// Crie uma tarefa para copiar todas as dependências de runtime
val copyDeps = tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("dependencies"))

    // Evita duplicar o que já vai estar no JRE via JLink
    exclude("org/openjfx/**")
}

tasks.register<Exec>("createInstallerLinux") {
    group = "distribution"
    description = "Gera o instalador .deb usando o script shell."

    dependsOn("jar", "copyDependencies")

    // Define o diretório de execução como a raiz do projeto
    workingDir = projectDir

    // Comando para rodar o script
    commandLine("bash", "./scripts/linux/create-installer-using-gradlew.sh")
}

tasks.register<Exec>("runInstallerLinux") {
    group = "distribution"
    description = "Executa a aplicação a partir da app-image gerada (sem precisar instalar)."

    val appName = props.getProperty("appName")
    commandLine("build/app-image/$appName/bin/$appName")
}

tasks.register<Exec>("createInstallerWindows") {
    group = "distribution"
    description = "Gera o instalador .msi usando o script PowerShell."

    dependsOn("jar", "copyDependencies")

    // Define o diretório de execução como a raiz do projeto
    workingDir = projectDir

    // Comando para rodar o script
    commandLine("pwsh", "./scripts/windows/create-installer-using-gradlew.ps1")
}


tasks.register<Exec>("createInstallerWindowsPython") {
    group = "distribution"
    description = "Gera o instalador .msi usando Python."

    dependsOn("jar", "copyDependencies")

    workingDir = projectDir

    commandLine("python", "./scripts/windows/create-installer.py")
}

tasks.register<Exec>("createFastExeWindows") {
    group = "distribution"
    description = "Gera o executável .exe rápido usando o script PowerShell."

    dependsOn("jar", "copyDependencies")

    workingDir = projectDir

    commandLine("pwsh", "./scripts/windows/create-fast-exe-using-gradlew.ps1")
}

// Configuração de Publicação (mantida)
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = props.getProperty("appName")
        }
    }
}

