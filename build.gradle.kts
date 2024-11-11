plugins {
    kotlin("jvm") version "1.8.20" // Confirme a versão do Kotlin que você está usando
    kotlin("plugin.serialization") version "1.8.20"  // Habilita o plugin de serialização
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor dependencies
    implementation("io.ktor:ktor-server-core:2.3.0")  // Versão mais recente do Ktor
    implementation("io.ktor:ktor-server-netty:2.3.0")  // Ktor com Netty
    implementation("io.ktor:ktor-server-content-negotiation:2.3.0")  // Para ContentNegotiation
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")  // Para a serialização JSON
    implementation("io.ktor:ktor-server-auth:2.3.0")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.0")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // Biblioteca de serialização Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")  // Versão compatível com o Ktor
}

kotlin {
    jvmToolchain(17) // Substitua para a versão JDK configurada no projeto
}
