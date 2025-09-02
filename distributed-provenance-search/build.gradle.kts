plugins {
    id("java")
    id("maven-publish")
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

    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")

    implementation("org.openprovenance.prov:prov-model:2.2.1")
    implementation("org.openprovenance.prov:prov-json:1.0.0")
    implementation("org.openprovenance.prov:prov-interop:2.2.1")
    implementation("org.openprovenance.prov:prov-nf:2.2.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.1")

    implementation("cz.muni.fi.cpm:cpm-core:1.0.0")
    implementation("cz.muni.fi.cpm:cpm-template:1.0.0")

    implementation("org.apache.httpcomponents:httpclient:4.5.13")
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