import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "io.github.alessandrocdrx.acertototal"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.alessandrocdrx.acertototal"
        minSdk = 29
        targetSdk = 36
        versionCode = 19
        versionName = "0.9.8"
    }

    // Assinatura: lida de android/assinatura.properties, que NÃO vai para o repositório
    // (chave e senha são segredos). Sem esse arquivo, o release sai com a chave de debug
    // do computador que compilou: nesse caso a versão nova pede desinstalar a anterior.
    val assinatura = rootProject.file("assinatura.properties")
    signingConfigs {
        if (assinatura.exists()) {
            val p = Properties().apply { assinatura.inputStream().use { load(it) } }
            create("release") {
                storeFile = rootProject.file(p.getProperty("arquivo"))
                storePassword = p.getProperty("senha")
                keyAlias = p.getProperty("alias")
                keyPassword = p.getProperty("senha")
            }
        }
    }

    val assinaturaPlay = rootProject.file("assinatura-play.properties")
    if (assinaturaPlay.exists()) {
        val p = Properties().apply { assinaturaPlay.inputStream().use { load(it) } }
        signingConfigs.create("play") {
            storeFile = rootProject.file(p.getProperty("arquivo"))
            storePassword = p.getProperty("senha")
            keyAlias = p.getProperty("alias")
            keyPassword = p.getProperty("senha")
        }
    }

    // Dois apps separados: o de teste (faixa "Ambiente de teste", ícone próprio)
    // e o de produção. Podem ficar instalados juntos no mesmo celular.
    flavorDimensions += "ambiente"
    productFlavors {
        create("homolog") {
            dimension = "ambiente"
            applicationIdSuffix = ".teste"
            versionNameSuffix = "-teste"
            resValue("string", "app_name", "acertoTotal Teste")
            buildConfigField("boolean", "COBRANCA_REAL", "false")
            // Dias de teste grátis (R67, R85): 7 no app de teste.
            buildConfigField("int", "LIMITE_DIAS", "7")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        create("producao") {
            dimension = "ambiente"
            resValue("string", "app_name", "acertoTotal")
            buildConfigField("boolean", "COBRANCA_REAL", "true")
            // 32 dias na fase de aprovação da Play, para a revisão e o teste fechado não travarem; 7 na versão final.
            buildConfigField("int", "LIMITE_DIAS", "32")
            // Google Play: assinado com a chave de envio (assinatura-play.properties, fora do repositório).
            signingConfigs.findByName("play")?.let { signingConfig = it }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.android.billingclient:billing:8.0.0")

    // Interface nativa (Jetpack Compose + Material 3) e banco de dados local (Room).
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // O Android já traz o leitor de XML; nos testes do computador ele precisa vir junto.
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    // O Android já traz o org.json (histórico das importações); nos testes do computador ele precisa vir junto.
    testImplementation("org.json:json:20231013")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // O Play Billing traz um Fragment antigo; a janela de arquivos (ActivityResult) exige 1.3.0 ou mais.
    implementation("androidx.fragment:fragment:1.8.5")
}
