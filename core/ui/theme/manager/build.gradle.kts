plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.core.ui.theme.manager"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(projects.core.ui.theme.api)

    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.core.preference.api)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
