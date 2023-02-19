import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.7.7"
	id("io.spring.dependency-management") version "1.0.15.RELEASE"
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"

	// apollo graphql の plugin を追加
	id("com.apollographql.apollo3").version("3.7.4")
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	// apollo graphql の ライブラリを追加
	runtimeOnly("com.apollographql.apollo3:apollo-api:3.7.4")

	// 非同期通信を扱うためのライブラリ
	implementation("com.apollographql.apollo3:apollo-rx3-support:3.7.4")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// apollo graphql の plugin 設定
apollo {
	service("sample-service") {
		packageName.set("com.example")

		introspection {
			endpointUrl.set("http://localhost:4000/graphql/endpoint")
			schemaFile.set(file("src/main/graphql/com/example/schema.graphqls"))
		}

	}
}