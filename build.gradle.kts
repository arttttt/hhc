plugins {
    alias(libs.plugins.kotlin.multiplatform)
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

        compilations.getByName("main") {
            cinterops {
                val uhid by creating {
                    defFile("src/nativeInterop/cinterop/uhid.def")
                }
            }
        }
    }

    sourceSets {
        val desktopMain by creating {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.kotlin.coroutines.core)
            }
        }

        nativeMain.get().dependsOn(desktopMain)
    }

    compilerOptions {
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
        optIn.add("kotlinx.coroutines.DelicateCoroutinesApi")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "8.7"
    distributionType = Wrapper.DistributionType.BIN
}
