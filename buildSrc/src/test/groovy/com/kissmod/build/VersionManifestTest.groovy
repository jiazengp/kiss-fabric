package com.kissmod.build

import org.gradle.api.GradleException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class VersionManifestTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void loadsEnabledLoaderCoordinatesAndCapabilities() {
        File manifestFile = writeManifest('''
minecraft_version=1.21.5
minecraft_version_range=[1.21.5,1.21.6)
java_version=21
enable_fabric=true
fabric_loader_version=0.16.14
fabric_api_version=0.128.1+1.21.5
fabric_permissions_api_version=0.3.3
enable_neoforge=false
neoforge_version=
neoforge_loader_version_range=
''')

        Map version = VersionManifest.load(manifestFile).versions[0]

        assertEquals('vtest', version.id)
        assertEquals(21, version.java_version)
        assertTrue(version.fabric.enabled)
        assertEquals('0.16.14', version.fabric.fabric_loader_version)
        assertEquals(['fabric'], VersionManifest.enabledLoaders(version))
        assertTrue(version.capabilities.fabric.modernGameTests)
    }

    @Test
    void rejectsInvalidBooleanValues() {
        File manifestFile = writeManifest('''
minecraft_version=1.21.5
minecraft_version_range=[1.21.5,1.21.6)
java_version=21
enable_fabric=yes
enable_neoforge=false
''')

        assertManifestFailure(manifestFile, "Invalid boolean value for version field 'enable_fabric'")
    }

    @Test
    void rejectsMissingCoordinatesForEnabledLoaders() {
        File manifestFile = writeManifest('''
minecraft_version=1.21.5
minecraft_version_range=[1.21.5,1.21.6)
java_version=21
enable_fabric=true
fabric_loader_version=0.16.14
fabric_api_version=
enable_neoforge=false
''')

        assertManifestFailure(manifestFile, "Enabled loader 'fabric' in version 'vtest' is missing 'fabric_api_version'")
    }

    @Test
    void rejectsJavaVersionsOutsideTheManifestPolicy() {
        File manifestFile = writeManifest('''
minecraft_version=26.2
minecraft_version_range=[26.2,26.3)
java_version=21
enable_fabric=false
enable_neoforge=false
''')

        assertManifestFailure(manifestFile, "Version 'vtest' must use java_version=25")
    }

    @Test
    void rejectsDuplicateVersionIds() {
        File manifestFile = new File(temporaryFolder.root, 'versions.json')
        manifestFile.text = '{"versions":["vtest","vtest"]}'

        assertManifestFailure(manifestFile, 'versions.json must not contain duplicate version IDs')
    }

    @Test
    void stagedVersionDirectoryPasses() {
        File manifestFile = writeManifest('''
minecraft_version=1.21.5
minecraft_version_range=[1.21.5,1.21.6)
java_version=21
enable_fabric=false
enable_neoforge=false
''')
        File stagedDir = new File(temporaryFolder.root, 'versions/vstaged')
        assertTrue(stagedDir.mkdirs())
        new File(stagedDir, 'gradle.properties').text =
                'minecraft_version=26.3-snapshot-2\njava_version=25\nenable_fabric=false\nenable_neoforge=false\n'

        Map version = VersionManifest.load(manifestFile).versions[0]
        assertEquals('vtest', version.id)
    }

    @Test
    void unregisteredVersionDirectoryWithEnabledLoadersFails() {
        File manifestFile = writeManifest('''
minecraft_version=1.21.5
minecraft_version_range=[1.21.5,1.21.6)
java_version=21
enable_fabric=false
enable_neoforge=false
''')
        File stagedDir = new File(temporaryFolder.root, 'versions/vstaged')
        assertTrue(stagedDir.mkdirs())
        new File(stagedDir, 'gradle.properties').text =
                'minecraft_version=26.3-snapshot-2\njava_version=25\nenable_fabric=true\nenable_neoforge=false\n'

        assertManifestFailure(manifestFile, 'not registered in versions.json')
    }

    private File writeManifest(String properties) {
        File versionDir = new File(temporaryFolder.root, 'versions/vtest')
        assertTrue(versionDir.mkdirs())
        new File(versionDir, 'gradle.properties').text = properties.stripIndent().trim() + '\n'
        File manifestFile = new File(temporaryFolder.root, 'versions.json')
        manifestFile.text = '{"versions":["vtest"]}'
        manifestFile
    }

    private static void assertManifestFailure(File manifestFile, String expectedMessage) {
        try {
            VersionManifest.load(manifestFile)
            fail('Expected manifest loading to fail')
        } catch (GradleException exception) {
            assertTrue(exception.message, exception.message.contains(expectedMessage))
        }
    }
}
