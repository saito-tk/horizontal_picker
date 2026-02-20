plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
    signing
}

android {
    namespace = "com.saitotk.horizontalpicker"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit4)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

group = "io.github.your-github-id"
version = "0.1.0"

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = project.group.toString()
                artifactId = "horizontal-picker"
                version = project.version.toString()

                pom {
                    name.set("Horizontal Picker")
                    description.set("A Jetpack Compose horizontal picker with center indicator and snapping.")
                    url.set("https://github.com/your-github-id/horizontal-picker")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("your-github-id")
                            name.set("Your Name")
                            email.set("you@example.com")
                        }
                    }
                    scm {
                        url.set("https://github.com/your-github-id/horizontal-picker")
                        connection.set("scm:git:git://github.com/your-github-id/horizontal-picker.git")
                        developerConnection.set("scm:git:ssh://git@github.com:your-github-id/horizontal-picker.git")
                    }
                }
            }
        }
    }
}

signing {
    isRequired = gradle.taskGraph.hasTask("publish")
    sign(publishing.publications)
}
