plugins {
    kotlin("multiplatform") version "2.0.0-RC3"
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    linuxX64("native") {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    sourceSets {
        val desktopMain by creating {
            dependsOn(commonMain.get())
        }

        nativeMain.get().dependsOn(desktopMain)
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "8.4.0"
    distributionType = Wrapper.DistributionType.BIN
}
