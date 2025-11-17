plugins {
    id("java")
    id("maven-publish")
    id("org.springframework.boot") version "3.4.+"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "cz.muni.xmichalk"
version = "1.0-SNAPSHOT"


repositories {
    mavenLocal()
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("corePublication") {
            groupId = "cz.muni.fi.cpm"
            artifactId = "cpm-core"
            version = "1.0.0"
            artifact(file("src/main/resources/cpm-core-1.0.0.jar"))
        }

        create<MavenPublication>("templatePublication") {
            groupId = "cz.muni.fi.cpm"
            artifactId = "cpm-template"
            version = "1.0.0"
            artifact(file("src/main/resources/cpm-template-1.0.0.jar"))
        }
    }

    repositories {
        mavenLocal()
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.openprovenance.prov:prov-model:2.2.1")
    implementation("org.openprovenance.prov:prov-json:1.0.0")
    implementation("org.openprovenance.prov:prov-interop:2.2.1")
    implementation("org.openprovenance.prov:prov-nf:2.2.1")

    implementation(files("src/main/resources/cpm-core-1.0.0.jar"))
    implementation(files("src/main/resources/cpm-template-1.0.0.jar"))


    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    implementation("org.apache.httpcomponents:httpclient:4.5.13")

    implementation("io.github.erdtman:java-json-canonicalization:1.1")
}

tasks.register("installCore") {
    dependsOn(tasks.named("publishToMavenLocal"))
}

tasks.register("installTemplate") {
    dependsOn(tasks.named("publishToMavenLocal"))
}

tasks.named("clean") {
    finalizedBy("installCore", "installTemplate")
}

tasks.test {
    useJUnitPlatform()
}