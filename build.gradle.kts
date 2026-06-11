// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Base64

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

tasks.register<Exec>("generateDebugKeystore") {
    val keystoreFile = file("debug.keystore")
    if (keystoreFile.exists()) {
        keystoreFile.delete()
    }
    commandLine(
        "keytool", "-genkey", "-v", 
        "-keystore", keystoreFile.absolutePath, 
        "-storepass", "android", 
        "-alias", "androiddebugkey", 
        "-keypass", "android", 
        "-keyalg", "RSA", 
        "-keysize", "2048", 
        "-validity", "10000", 
        "-dname", "CN=Android Debug,O=Android,C=US"
    )
}

tasks.register("encodeKeystore") {
    dependsOn("generateDebugKeystore")
    doLast {
        val keystoreFile = file("debug.keystore")
        val base64File = file("debug.keystore.base64")
        val bytes = keystoreFile.readBytes()
        val base64Text = java.util.Base64.getEncoder().encodeToString(bytes)
        base64File.writeText(base64Text)
        println("SUCCESS: debug.keystore encoded to debug.keystore.base64")
    }
}


