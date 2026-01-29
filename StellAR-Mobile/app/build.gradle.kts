import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.2.21-2.0.4"
    id("com.google.dagger.hilt.android") version "2.57.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

android {
    namespace = "com.cosmic_struck.stellar"
    compileSdk {
        version = release(36)
    }


    defaultConfig {
        applicationId = "com.cosmic_struck.stellar"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a")
        }


        val file = rootProject.file("secrets.properties")
        val properties = Properties()
        properties.load(file.inputStream())
        buildConfigField("String", "SUPABASE_URL", properties.getProperty("SUPABASE_URL"))
        buildConfigField("String", "SUPABASE_KEY", properties.getProperty("SUPABASE_KEY"))
        buildConfigField("String","ARCORE_KEY",properties.getProperty("ARCORE_KEY"))
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
    buildFeatures {
        compose = true
        prefab = true
        buildConfig = true
    }
}

dependencies {
    //Dagger Hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.compose.foundation)
    ksp("com.google.dagger:hilt-android-compiler:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    //Navigation
    implementation("androidx.navigation:navigation-compose:2.9.6")

    //landscapist
    implementation("com.github.skydoves:landscapist-coil:2.6.1")

    //SplashScreen
    implementation("androidx.core:core-splashscreen:1.2.0")

    //Work
    implementation("androidx.work:work-runtime-ktx:2.11.0")

    //Exoplayer
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.7.1")
    implementation("androidx.media3:media3-ui:1.8.0")

    //Lottie
    implementation("com.airbnb.android:lottie-compose:6.7.1")

    //CameraX
    val camerax_version = "1.6.0-alpha01"
    implementation("androidx.camera:camera-camera2:${camerax_version}")
// If you want to additionally use the CameraX Lifecycle library
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
// If you want to additionally use the CameraX VideoCapture library
    implementation("androidx.camera:camera-video:${camerax_version}")
// If you want to additionally use the CameraX View class
    implementation("androidx.camera:camera-view:${camerax_version}")
// If you want to additionally add CameraX ML Kit Vision Integration
    implementation("androidx.camera:camera-mlkit-vision:${camerax_version}")
// If you want to additionally use the CameraX Extensions library
    implementation("androidx.camera:camera-extensions:${camerax_version}")

    //DATA-STORE
    implementation("androidx.datastore:datastore:1.2.0")
    implementation("androidx.datastore:datastore-preferences-android:1.2.0")

    //ROOM
    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.room:room-paging:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")

    //PERMISSIONS
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    //SWIPE-ABLE
    implementation("me.saket.swipe:swipe:1.3.0")

    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    //Paging
    implementation("androidx.paging:paging-compose:3.3.6")

    //Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")


    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.6"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    implementation("dev.chrisbanes.haze:haze-jetpack-compose:0.4.1")
    implementation("io.github.sceneview:sceneview:2.3.1")
    implementation("io.github.sceneview:arsceneview:2.3.2")
    implementation("io.ktor:ktor-client-okhttp:3.3.3")

    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    implementation("com.google.accompanist:accompanist-swiperefresh:0.36.0")
    implementation("io.coil-kt:coil-compose:2.4.0")
// OkHttp for HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    implementation(project(":chemLab"))
    implementation(fileTree(mapOf(
        "dir" to "${rootProject.project(":chemLab").projectDir}/libs",
        "include" to listOf("*.jar")
    )))
    implementation("androidx.games:games-activity:4.0.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}