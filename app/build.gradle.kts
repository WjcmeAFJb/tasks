@file:Suppress("UnstableApiUsage")

import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("android")
    id("dagger.hilt.android.plugin")
    id("com.google.android.gms.oss-licenses-plugin")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

composeCompiler {
    enableStrongSkippingMode = true
}

android {
    bundle {
        language {
            enableSplit = false
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
        buildConfig = true
    }

    lint {
        lintConfig = file("lint.xml")
        textOutput = File("stdout")
        textReport = true
    }

    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        testApplicationId = "org.tasks.test"
        applicationId = "org.tasks"
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "org.tasks.TestRunner"
    }

    signingConfigs {
        create("release") {
            val tasksKeyAlias: String? by project
            val tasksStoreFile: String? by project
            val tasksStorePassword: String? by project
            val tasksKeyPassword: String? by project

            keyAlias = tasksKeyAlias
            storeFile = file(tasksStoreFile ?: "none")
            storePassword = tasksStorePassword
            keyPassword = tasksKeyPassword
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    flavorDimensions += listOf("store")

    @Suppress("LocalVariableName")
    buildTypes {
        debug {
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
            val tasks_mapbox_key_debug: String? by project
            val tasks_google_key_debug: String? by project
            resValue("string", "mapbox_key", tasks_mapbox_key_debug ?: "")
            resValue("string", "google_key", tasks_google_key_debug ?: "")
            resValue("string", "posthog_key", "")
            enableUnitTestCoverage = project.hasProperty("coverage")
            // enableAndroidTestCoverage is blocked by :icons module
            // (OutlinedGoogleMaterial$Icon.<clinit> exceeds 64KB after JaCoCo)
        }
        release {
            val tasks_mapbox_key: String? by project
            val tasks_google_key: String? by project
            val tasks_posthog_key: String? by project
            resValue("string", "mapbox_key", tasks_mapbox_key ?: "")
            resValue("string", "google_key", tasks_google_key ?: "")
            resValue("string", "posthog_key", tasks_posthog_key ?: "")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    productFlavors {
        create("generic") {
            dimension = "store"
        }
        create("googleplay") {
            isDefault = true
            dimension = "store"
        }
    }
    packaging {
        resources {
            excludes += setOf("META-INF/*.kotlin_module", "META-INF/INDEX.LIST")
        }
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel2api30") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }

    namespace = "org.tasks"
}

configurations.all {
    exclude(group = "org.apache.httpcomponents")
    exclude(group = "org.checkerframework")
    exclude(group = "com.google.code.findbugs")
    exclude(group = "com.google.errorprone")
    exclude(group = "com.google.j2objc")
    exclude(group = "com.google.http-client", module = "google-http-client-apache-v2")
    exclude(group = "com.google.http-client", module = "google-http-client-jackson2")
}

val genericImplementation by configurations
val googleplayImplementation by configurations

dependencies {
    implementation(projects.data)
    implementation(projects.kmp)
    implementation(libs.kermit)
    implementation(projects.icons)
    implementation(libs.androidx.navigation)
    implementation(libs.androidx.adaptive.navigation.android)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.bitfire.ical4android) {
        exclude(group = "commons-logging")
        exclude(group = "org.json", module = "json")
        exclude(group = "org.codehaus.groovy", module = "groovy")
        exclude(group = "org.codehaus.groovy", module = "groovy-dateutil")
    }
    implementation(libs.bitfire.cert4android)
    implementation(libs.dmfs.opentasks.provider) {
        exclude("com.github.tasks.opentasks", "opentasks-contract")
    }
    implementation(libs.dmfs.rfc5545.datetime)
    implementation(libs.dmfs.recur)
    implementation(libs.dmfs.jems)

    implementation(libs.dagger.hilt)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation)
    implementation(libs.androidx.hilt.work)

    implementation(libs.androidx.core.remoteviews)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.room)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.appcompat)
    implementation(libs.iconics)
    implementation(libs.markwon)
    implementation(libs.markwon.editor)
    implementation(libs.markwon.linkify)
    implementation(libs.markwon.strikethrough)
    implementation(libs.markwon.tables)
    implementation(libs.markwon.tasklist)

    debugImplementation(libs.leakcanary)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation(libs.kotlin.reflect)

    implementation(libs.kotlin.jdk8)
    implementation(libs.kotlinx.immutable)
    implementation(libs.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.persistent.cookiejar)
    implementation(libs.material)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.preference)
    implementation(libs.timber)
    implementation(libs.dashclock.api)
    implementation(libs.locale) {
        isTransitive = false
    }
    implementation(libs.jchronic) {
        isTransitive = false
    }
    implementation(libs.shortcut.badger)
    implementation(libs.google.api.tasks)
    implementation(libs.google.api.drive)
    implementation(libs.google.oauth2)
    implementation(libs.androidx.work)
    implementation(libs.etebase)
    implementation(libs.colorpicker)
    implementation(libs.appauth)
    implementation(libs.osmdroid)
    implementation(libs.androidx.recyclerview)

    implementation(platform(libs.androidx.compose))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)

    implementation(libs.ktor)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.serialization)

    implementation(libs.accompanist.permissions)

    googleplayImplementation(platform(libs.firebase))
    googleplayImplementation(libs.firebase.crashlytics)
    googleplayImplementation(libs.posthog.android)
    googleplayImplementation(libs.firebase.config)
    googleplayImplementation(libs.firebase.messaging)
    googleplayImplementation(libs.play.services.location)
    googleplayImplementation(libs.play.services.maps)
    googleplayImplementation(libs.play.billing.ktx)
    googleplayImplementation(libs.play.review)
    googleplayImplementation(libs.play.services.oss.licenses)
    googleplayImplementation(libs.horologist.datalayer.phone)
    googleplayImplementation(libs.horologist.datalayer.grpc)
    googleplayImplementation(libs.horologist.datalayer.core)
    googleplayImplementation(libs.play.services.wearable)
    googleplayImplementation(libs.microsoft.authentication) {
        exclude("com.microsoft.device.display", "display-mask")
    }
    googleplayImplementation(projects.wearDatalayer)

    androidTestImplementation(libs.dagger.hilt.testing)
    kspAndroidTest(libs.dagger.hilt.compiler)
    kspAndroidTest(libs.androidx.hilt.compiler)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.make.it.easy)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.okhttp.mockwebserver)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.make.it.easy)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.xpp3)
}

// ---------------------------------------------------------------------------
// JaCoCo coverage report — merges unit + instrumented + E2E execution data.
//
//   ./gradlew -Pcoverage :app:testGenericDebugUnitTest :app:jacocoReport
//   ./gradlew -Pcoverage :app:connectedGenericDebugAndroidTest :app:jacocoReport
//
// Reports land in app/build/reports/jacoco/jacocoReport/
// ---------------------------------------------------------------------------
if (project.hasProperty("coverage")) {
    apply(plugin = "jacoco")

    tasks.register<JacocoReport>("jacocoReport") {
        group = "Verification"
        description = "Generates merged JaCoCo coverage report (unit + instrumented)."

        reports {
            html.required.set(true)
            xml.required.set(true)
            csv.required.set(false)
        }

        val classExcludes = listOf(
            "**/R.class", "**/R\$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/databinding/**",
        )

        // Use the same class directories that AGP's JaCoCo instrumentation uses.
        // App: post-ASM-transform classes (class IDs match the instrumented APK).
        // KMP/Data: bundled runtime classes (consumed by app's JaCoCo transform).
        val appClasses = fileTree(
            "${layout.buildDirectory.get()}/intermediates/classes/genericDebug/transformGenericDebugClassesWithAsm/dirs"
        )
        val kmpClasses = fileTree(
            "${project(":kmp").layout.buildDirectory.get()}/intermediates/runtime_library_classes_dir/debug/bundleLibRuntimeToDirDebug"
        )
        val dataClasses = fileTree(
            "${project(":data").layout.buildDirectory.get()}/intermediates/runtime_library_classes_dir/debug/bundleLibRuntimeToDirDebug"
        )

        classDirectories.setFrom(
            appClasses.matching { exclude(classExcludes) },
            kmpClasses.matching { exclude(classExcludes) },
            dataClasses.matching { exclude(classExcludes) },
        )

        sourceDirectories.setFrom(files(
            "src/main/java",
            "src/generic/java",
            "${project(":kmp").projectDir}/src/commonMain/kotlin",
            "${project(":kmp").projectDir}/src/jvmCommonMain/kotlin",
            "${project(":kmp").projectDir}/src/androidMain/kotlin",
            "${project(":data").projectDir}/src/commonMain/kotlin",
            "${project(":data").projectDir}/src/androidMain/kotlin",
        ))

        // Collect .exec (unit tests) and .ec (instrumented/E2E) files
        executionData.setFrom(fileTree(layout.buildDirectory) {
            include(
                "outputs/unit_test_code_coverage/**/*.exec",
                "outputs/code_coverage/**/*.ec",
                "jacoco/*.ec",
                "jacoco/*.exec",
            )
        })
    }
}

// ---------------------------------------------------------------------------
// Mutation testing with PIT (pitest).
//
//   ./gradlew pitestMutationTesting
//
// PIT does not natively support Android modules, so this task runs PIT
// as a Java process against the app's compiled Kotlin classes and unit tests.
// Uses pitest-kotlin-plugin to filter out Kotlin compiler-generated mutations.
//
// Reports land in app/build/reports/pitest/
// ---------------------------------------------------------------------------
val pitestClasspath: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
dependencies {
    pitestClasspath("org.pitest:pitest-command-line:1.17.4")
    pitestClasspath("org.pitest:pitest:1.17.4")
}

tasks.register<JavaExec>("pitestMutationTesting") {
    group = "Verification"
    description = "Runs PIT mutation testing on app unit tests."
    dependsOn("testGenericDebugUnitTest")

    mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
    classpath = pitestClasspath

    val testTask = tasks.named<Test>("testGenericDebugUnitTest")
    val reportDir = layout.buildDirectory.dir("reports/pitest")

    doFirst {
        val buildDir = layout.buildDirectory.get().asFile.absolutePath
        val appClasses = "$buildDir/tmp/kotlin-classes/genericDebug"
        val testClasses = "$buildDir/intermediates/classes/genericDebugUnitTest/transformGenericDebugUnitTestClassesWithAsm/dirs"

        // PIT classPath must include: app classes, test classes, and all deps
        val cpEntries = mutableListOf(appClasses, testClasses)
        testTask.get().classpath.files.forEach { cpEntries.add(it.absolutePath) }
        val fullCp = cpEntries.joinToString(",")

        args(
            "--reportDir", reportDir.get().asFile.absolutePath,
            "--sourceDirs", file("src/main/java").absolutePath + "," + file("src/generic/java").absolutePath,
            "--targetClasses", "org.tasks.repeats.*,org.tasks.caldav.*,org.tasks.notifications.*,org.tasks.location.*,org.tasks.data.entity.*,org.tasks.data.sql.*,org.tasks.security.*,org.tasks.sync.microsoft.*,org.tasks.time.*,org.tasks.filters.*,org.tasks.preferences.*,com.todoroo.astrid.repeats.*,com.todoroo.astrid.alarms.*",
            "--excludedClasses", "*Test,*Tests,*Test$*,*Tests$*,*Maker*,*TestCase*,*TestUtilities*,*Activity,*Fragment,*ControlSet,*Screen,*Dialog,*Adapter,*ViewHolder",
            "--targetTests", "org.tasks.*,com.todoroo.*",
            "--classPath", fullCp,
            "--mutators", "STRONGER",
            "--outputFormats", "HTML,XML",
            "--timestampedReports", "false",
            "--threads", "4",
            "--timeoutConst", "8000",
            "--verbose",
        )
    }
}
