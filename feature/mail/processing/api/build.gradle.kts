plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.mail.processing.api"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
