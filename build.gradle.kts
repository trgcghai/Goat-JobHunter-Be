plugins {
	java
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("io.freefair.lombok") version "8.6"
}

group = "fit.se"
version = "0.0.1-SNAPSHOT"
description = "Goat - Platform tim kiem viec lam spring boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") {
		exclude(group = "com.google.code.gson", module = "gson")
	}
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
	implementation("com.turkraft.springfilter:jpa:3.2.1")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")
	runtimeOnly ("com.h2database:h2")
	implementation("org.springframework.boot:spring-boot-starter-data-redis:3.5.7")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.mysql:mysql-connector-j")
	implementation("mysql:mysql-connector-java:8.0.33")

	// https://mvnrepository.com/artifact/com.cloudinary/cloudinary-http44
	implementation ("com.cloudinary:cloudinary-http44:1.39.0")
	// https://mvnrepository.com/artifact/commons-io/commons-io
	implementation("commons-io:commons-io:2.19.0")

	// https://mvnrepository.com/artifact/commons-net/commons-net
	implementation("commons-net:commons-net:3.12.0")
	// https://mvnrepository.com/artifact/dnsjava/dnsjava
	implementation("dnsjava:dnsjava:3.6.3")

	implementation("net.datafaker:datafaker:2.5.3")

	// https://mvnrepository.com/artifact/org.springframework/spring-webflux
	implementation("org.springframework:spring-webflux:7.0.1")

	// https://mvnrepository.com/artifact/com.google.genai/google-genai
	implementation("com.google.genai:google-genai:1.28.0")

    implementation("org.springframework.boot:spring-boot-starter-websocket")

    implementation("org.springframework.boot:spring-boot-starter-security")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
