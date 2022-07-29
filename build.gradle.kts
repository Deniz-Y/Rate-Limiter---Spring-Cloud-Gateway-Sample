import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id ("com.google.cloud.tools.jib") version "3.2.1"
	id("org.springframework.boot") version "2.7.1"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	id("org.jmailen.kotlinter") version "3.9.0"
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
}

group = "com.valensas"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

extra["springCloudVersion"] = "2021.0.3"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("org.springframework.cloud:spring-cloud-starter-gateway")
	implementation("org.springframework.cloud:spring-cloud-starter-sleuth")
	implementation("io.micrometer:micrometer-registry-prometheus")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")

	// Kubernetes
	implementation("io.kubernetes:client-java-extended:15.0.1")

	implementation("com.hazelcast:hazelcast-all:4.2.5")
	implementation("com.github.vladimir-bukhtoyarov:bucket4j-hazelcast:7.5.0")
	implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.5.0")
	implementation("com.github.vladimir-bukhtoyarov:bucket4j-jcache:7.5.0")

	// M1 Support
	runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.77.Final:osx-aarch_64")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
