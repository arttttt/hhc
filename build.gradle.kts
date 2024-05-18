plugins {
    alias(libs.plugins.jetbrains.kotlin.multiplatform)
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
    gradleVersion = "8.7"
    distributionType = Wrapper.DistributionType.BIN
}
