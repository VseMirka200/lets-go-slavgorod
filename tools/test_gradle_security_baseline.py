import unittest

from check_gradle_security_baseline import audit, extract_modules, numeric_version


class DependencyAuditTests(unittest.TestCase):
    def test_requested_legacy_version_is_not_a_selected_version(self):
        tree = "|    +--- com.google.guava:guava:18.0 -> 33.8.0-jre (*)"
        self.assertIn(("com.google.guava", "guava", "33.8.0-jre"), extract_modules(tree))
        self.assertNotIn(("com.google.guava", "guava", "18.0"), extract_modules(tree))

    def test_bom_constraint_is_ignored(self):
        self.assertEqual(extract_modules("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10 (c)"), set())

    def test_numeric_versions_keep_all_components(self):
        self.assertLess(numeric_version("2.0.6"), numeric_version("2.0.6.1"))

    def test_detects_old_plugin_transitives(self):
        errors = audit(
            "org.jetbrains.kotlin:kotlin-stdlib:2.4.21",
            "org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21\n"
            "org.bouncycastle:bcprov-jdk18on:1.80.2\n"
            "org.jdom:jdom2:2.0.6\n"
        )
        self.assertTrue(any("bcprov" in value for value in errors))
        self.assertTrue(any("jdom2" in value for value in errors))

    def test_rejects_flagged_module_in_release_runtime(self):
        errors = audit(
            "com.google.guava:guava:33.8.0-jre",
            "org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21",
        )
        self.assertTrue(any("release runtime" in value for value in errors))

    def test_rejects_vulnerable_kotlin_compiler_plugin(self):
        errors = audit(
            "org.jetbrains.kotlin:kotlin-stdlib:2.4.21",
            "org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21",
            "org.bouncycastle:bcprov-jdk18on:1.76",
        )
        self.assertTrue(any("compiler plugin" in value for value in errors))

    def test_accepts_patched_compiler_plugin(self):
        errors = audit(
            "org.jetbrains.kotlin:kotlin-stdlib:2.4.21",
            "org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21",
            "org.bouncycastle:bcprov-jdk18on:1.76 -> 1.85\n"
            "com.google.guava:guava:31.1-jre -> 33.8.0-jre",
        )
        self.assertEqual(errors, [])

    def test_accepts_patched_build_classpath(self):
        report = "\n".join([
            "org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10 -> 2.4.21",
            "org.bouncycastle:bcprov-jdk18on:1.76 -> 1.85",
            "org.bouncycastle:bcutil-jdk18on:1.85",
            "com.google.guava:guava:18.0 -> 33.8.0-jre",
            "org.apache.commons:commons-lang3:3.18.0",
            "org.bitbucket.b_c:jose4j:0.9.6",
            "org.jdom:jdom2:2.0.6.1",
            "org.apache.httpcomponents:httpclient:4.5.14",
        ])
        self.assertEqual(audit("com.google.guava:listenablefuture:1.0", report), [])


if __name__ == "__main__":
    unittest.main()
