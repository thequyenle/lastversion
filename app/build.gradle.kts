plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    // Loại bỏ androidx.room plugin vì có thể conflict với KSP
}

android {
    namespace = "net.android.lastversion"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.android.lastversion"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // Preference
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // Room - Sử dụng KSP thay vì kapt
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.databinding.runtime)
    ksp(libs.androidx.room.compiler)

    implementation("io.github.ShawnLin013:number-picker:2.4.13")
    // SQLite bundled để tránh lỗi OEM driver
    // implementation(libs.androidx.sqlite.bundled)
    implementation(libs.sqlite)
    implementation(libs.sqlite.ktx)
    implementation(libs.sqlite.framework)
    implementation(libs.material)
    // Coroutines - Cần thiết cho Room
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")


    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Lifecycle components - Cần cho Flow và ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Fragment KTX cho lifecycleScope
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    implementation("com.tbuonomo:dotsindicator:4.3")
    implementation("com.google.android.material:material:1.10.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Parcelize
    implementation("org.jetbrains.kotlin:kotlin-parcelize-runtime:1.9.22")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}