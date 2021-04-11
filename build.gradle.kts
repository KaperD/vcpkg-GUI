plugins {
    java
    application
}

group = "ru.hse"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}


tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "ru.hse.gui.MainWindow"
    }
}

application {
    mainClass.set("ru.hse.gui.MainWindow")
}

tasks.compileJava {
    options.release.set(11)
}