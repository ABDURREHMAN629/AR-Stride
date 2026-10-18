import java.util.Properties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * Release signing is read from keystore.properties at the project root, which is not in
 * version control. Copy keystore.properties.example, point it at your own keystore, and
 * `assembleRelease` signs with it; without that file the release build is left unsigned.
 */
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val releaseStore = keystoreProps.getProperty("storeFile")?.let { rootProject.file(it) }
val hasReleaseKeystore = releaseStore?.exists() == true

android {
    namespace = "com.stride.fit"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.stride.fit"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.3"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = releaseStore
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}


ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.services.location)

    testImplementation("junit:junit:4.13.2")
}
