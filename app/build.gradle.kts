plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.suchi.musicisland"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.suchi.musicisland"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.media3.session)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    annotationProcessor("com.github.Raizlabs.DBFlow:dbflow-processor:4.2.4")
    implementation("com.github.Raizlabs.DBFlow:dbflow:4.2.4")
    implementation("com.github.Raizlabs.DBFlow:dbflow-core:4.2.4")

    implementation(files("libs/jaudiotagger-2.2.6-SNAPSHOT.jar"))

    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
    implementation("com.github.bumptech.glide:glide:4.15.1")

    implementation("androidx.recyclerview:recyclerview:1.3.1")

    implementation("androidx.media3:media3-exoplayer:1.9.0")
    implementation("androidx.media3:media3-ui:1.9.0")
    implementation("androidx.media3:media3-common:1.9.0")
}