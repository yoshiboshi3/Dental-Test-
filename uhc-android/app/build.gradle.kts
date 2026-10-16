plugins {
    id("com.android.application")
}

android {
    namespace = "dev.tilm.uhcandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.tilm.uhcandroid"
        minSdk = 21
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.2"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
