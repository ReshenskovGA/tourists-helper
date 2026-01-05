plugins {
    alias(libs.plugins.kotlin.compose)
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("org.jetbrains.dokka")
}

android {
    namespace = "com.example.touristassistant"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.touristassistant"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Maps
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Location
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Networking
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Для работы с корутинами
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // OpenRouteService для построения маршрутов
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Dokka для документации (для генерации Javadoc/Jekyll)
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.9.10")

}

kapt {
    correctErrorTypes = true
}

// Настройка Dokka
tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("dokka"))

    moduleName.set("TouristAssistant")

    dokkaSourceSets {
        named("main") {
            // Указываем исходные директории
            sourceRoots.from(
                file("src/main/java"),
                file("src/main/kotlin")
            )

            // Настраиваем внешние ссылки
            externalDocumentationLink {
                url.set(uri("https://developer.android.com/reference/").toURL())
                packageListUrl.set(uri("https://developer.android.com/reference/androidx/package-list").toURL())
            }

            externalDocumentationLink {
                url.set(uri("https://kotlinlang.org/api/latest/jvm/stdlib/").toURL())
            }

            // Исключаем сгенерированные файлы Dagger/Hilt
            perPackageOption {
                matchingRegex.set(".*\\.di\\..*")
                suppress.set(true)
            }

            perPackageOption {
                matchingRegex.set(".*\\.generated\\..*")
                suppress.set(true)
            }

            // Настраиваем документацию для Android классов
            jdkVersion.set(17)

            // Включаем все типы видимости
            includeNonPublic.set(false)

            // Показывать предупреждения
            reportUndocumented.set(true)

            // Копировать файлы документации
            skipEmptyPackages.set(true)

            // Включаем deprecated API
            skipDeprecated.set(false)
        }
    }

    // Настройки плагинов
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """{
                "customStyleSheets": [],
                "customAssets": [],
                "footerMessage": "© 2024 Tourist Assistant",
                "separateInheritedMembers": true
            }"""
        )
    )
}

// Задача для генерации документации в формате Javadoc
tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

// Задача для генерации документации в формате HTML
tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

// Задача для генерации документации в формате Markdown (для GitHub)
tasks.register<Jar>("dokkaGfmJar") {
    dependsOn(tasks.dokkaGfm)
    from(tasks.dokkaGfm.flatMap { it.outputDirectory })
    archiveClassifier.set("gfm-doc")
}

// Задача для генерации всех видов документации
tasks.register("generateDocumentation") {
    group = "documentation"
    description = "Генерирует все виды документации"

    dependsOn(
        "dokkaHtml",
        "dokkaJavadoc",
        "dokkaGfm",
        "dokkaHtmlJar",
        "dokkaJavadocJar",
        "dokkaGfmJar"
    )
}