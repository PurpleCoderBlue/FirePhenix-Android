plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.mail.processing.internal"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.feature.mail.processing.api)
    implementation(projects.core.ui.theme.api)
    implementation(projects.core.ui.contract)
    implementation(projects.core.logging.api)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(projects.core.ui.compose.testing)
}
