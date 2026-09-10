// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

// Redirect build output to a local folder outside of OneDrive to prevent AccessDenied errors
allprojects {
    layout.buildDirectory.set(file("C:/temp/android-builds/${rootProject.name}/${project.name}"))
}
