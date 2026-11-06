// Override vulnerable transitive dependencies of Gradle plugins.
// This affects build tooling, not the Android application's runtime classpath.
// Keep versions in sync with the security audit and verification metadata.
buildscript {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            val coordinate = "${requested.group}:${requested.name}"
            when (coordinate) {
                "com.google.guava:guava" -> useVersion("33.8.0-jre")
                "org.apache.commons:commons-lang3" -> useVersion("3.18.0")
                "org.bitbucket.b_c:jose4j" -> useVersion("0.9.6")
                "org.jdom:jdom2" -> useVersion("2.0.6.1")
                "org.apache.httpcomponents:httpclient" -> useVersion("4.5.14")
            }
            if (requested.group == "org.bouncycastle" && requested.name.endsWith("-jdk18on")) {
                useVersion("1.85")
            }
        }
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.detekt) apply false
}
