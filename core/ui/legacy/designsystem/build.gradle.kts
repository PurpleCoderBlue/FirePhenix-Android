plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.core.ui.legacy.designsystem"
}

dependencies {
    api(projects.core.ui.legacy.theme2.common)

    testImplementation(libs.robolectric)
}

codeCoverage {
    lineCoverage = 0
}
