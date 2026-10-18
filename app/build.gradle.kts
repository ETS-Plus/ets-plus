import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.etsprivate"
    compileSdk = 37

    defaultConfig {
        ndk { abiFilters += listOf("arm64-v8a") }
        applicationId = "ca.synocti.etsplus"
        minSdk = 26
        targetSdk = 37

        versionCode = 10102
        versionName = "1.1.2"
    }

    fun key(name: String) = File(System.getProperty("user.home"), ".android/$name.properties").takeIf { it.exists() }
        ?.let { file -> Properties().apply { file.inputStream().use(::load) } }
    val releaseKey = key("ets-plus-release")
    signingConfigs {
        if (releaseKey != null) {
            create("githubRelease") {
                storeFile = file(releaseKey.getProperty("storeFile"))
                storePassword = releaseKey.getProperty("storePassword")
                keyAlias = releaseKey.getProperty("keyAlias")
                keyPassword = releaseKey.getProperty("keyPassword")
            }
        }
    }

    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            buildConfigField("boolean", "STORE_BUILD", "false")
            signingConfig = signingConfigs.findByName("githubRelease") ?: signingConfigs.getByName("debug")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes.add("META-INF/NOTICE.md")
        resources.excludes.add("META-INF/LICENSE.md")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")
    implementation(composeBom)

    implementation("org.maplibre.gl:android-sdk:13.6.1")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation(composeBom)
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("androidx.glance:glance-appwidget:1.1.1")

    implementation("androidx.webkit:webkit:1.12.1")

    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
}

tasks.register<Jar>("pluginApi") {
    dependsOn("compileGithubReleaseKotlin", "compileGithubReleaseJavaWithJavac")
    archiveFileName.set("ets-plus-api.jar")
    destinationDirectory.set(layout.buildDirectory.dir("outputs/api"))
    from(layout.buildDirectory.dir("intermediates/built_in_kotlinc/githubRelease/compileGithubReleaseKotlin/classes"))
    from(layout.buildDirectory.dir("intermediates/javac/githubRelease/compileGithubReleaseJavaWithJavac/classes"))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
