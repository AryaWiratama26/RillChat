plugins {
    alias(libs.plugins.android.application)

    // Google Services
    alias(libs.plugins.google.gms.google.services)


}

android {
    namespace = "com.example.rillchat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.rillchat"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Scalabe Size Unit (Support for different screen sizes)
    implementation (libs.sdp.android)
    implementation (libs.ssp.android)

    // Rounded ImageView
    implementation (libs.roundedimageview)

    // Firebase Google
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.firestore)

    // Multidex
    implementation(libs.multidex)



}
