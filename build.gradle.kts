// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

import java.util.Base64
import java.security.KeyStore
import java.io.FileInputStream

tasks.register("restoreKeystore") {
    doLast {
        val base64File = file("debug.keystore.base64")
        if (base64File.exists()) {
            val base64Str = base64File.readText().trim()
            val decodedBytes = Base64.getDecoder().decode(base64Str)
            val outputFile = file("debug.keystore")
            outputFile.writeBytes(decodedBytes)
            println("Successfully decoded debug.keystore.base64 to ${outputFile.absolutePath}")
        } else {
            println("No debug.keystore.base64 found to decode!")
        }
    }
}

tasks.register("verifyKeystore") {
    doLast {
        val keystoreFile = file("debug.keystore")
        if (!keystoreFile.exists()) {
            throw GradleException("Keystore file debug.keystore does not exist in root!")
        }
        try {
            val keyStore = KeyStore.getInstance("PKCS12") // standard modern or JKS
            FileInputStream(keystoreFile).use { fis ->
                keyStore.load(fis, "android".toCharArray())
            }
            if (keyStore.containsAlias("androiddebugkey")) {
                println("SUCCESS: debug.keystore is valid and has alias 'androiddebugkey'!")
            } else {
                val aliases = keyStore.aliases().asSequence().toList()
                println("WARNING: debug.keystore is valid but does NOT have alias 'androiddebugkey'. Available aliases: $aliases")
            }
        } catch (e: Exception) {
            println("Keystore load failed as PKCS12, trying JKS...")
            try {
                val keyStore = KeyStore.getInstance("JKS")
                FileInputStream(keystoreFile).use { fis ->
                    keyStore.load(fis, "android".toCharArray())
                }
                if (keyStore.containsAlias("androiddebugkey")) {
                    println("SUCCESS: debug.keystore JKS is valid and has alias 'androiddebugkey'!")
                } else {
                    val aliases = keyStore.aliases().asSequence().toList()
                    println("WARNING: debug.keystore is valid but does NOT have alias 'androiddebugkey'. Available aliases: $aliases")
                }
            } catch (e2: Exception) {
                throw GradleException("Keystore load failed: ${e2.message}", e2)
            }
        }
    }
}









