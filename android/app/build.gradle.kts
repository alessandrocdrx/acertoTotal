import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "br.com.celsopix"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.celsopix"
        minSdk = 29
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0"
    }

    // Dois apps separados: o de teste (faixa "Ambiente de teste", ícone próprio)
    // e o de produção. Podem ficar instalados juntos no mesmo celular.
    flavorDimensions += "ambiente"
    productFlavors {
        create("homolog") {
            dimension = "ambiente"
            applicationIdSuffix = ".teste"
            versionNameSuffix = "-teste"
            resValue("string", "app_name", "CelsoPix Teste")
        }
        create("producao") {
            dimension = "ambiente"
            resValue("string", "app_name", "CelsoPix")
        }
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

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
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
    implementation("androidx.webkit:webkit:1.12.1")
}
