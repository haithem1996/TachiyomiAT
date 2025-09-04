plugins {
    `kotlin-dsl`
}

dependencies {
implementation "com.squareup.okhttp3:okhttp:4.10.0"
implementation "androidx.room:room-runtime:2.5.1"
kapt "androidx.room:room-compiler:2.5.1"
implementation "androidx.room:room-ktx:2.5.1"
    implementation(androidx.gradle)
    implementation(kotlinx.gradle)
    implementation(kotlinx.compose.compiler.gradle)
    implementation(libs.spotless.gradle)
    implementation(gradleApi())

    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(androidx.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(compose.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(kotlinx.javaClass.superclass.protectionDomain.codeSource.location))
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}
