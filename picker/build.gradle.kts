plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
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
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit4)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("io.github.saito-tk", "horizontal-picker", "0.1.0")

    pom {
        name.set("Horizontal / Vertical Picker for Jetpack Compose")
        description.set(
            "A ruler-style horizontal and vertical picker library for Jetpack Compose."
        )
        inceptionYear.set("2026")
        url.set("https://github.com/saito-tk/horizontal_picker")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit/")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("saito-tk")
                name.set("saito-tk")
                url.set("https://github.com/saito-tk")
            }
        }

        scm {
            url.set("https://github.com/saito-tk/horizontal_picker")
            connection.set("scm:git:git://github.com/saito-tk/horizontal_picker.git")
            developerConnection.set("scm:git:ssh://git@github.com:saito-tk/horizontal_picker.git")
        }
    }
}
