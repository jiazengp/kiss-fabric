package com.kissmod.build

import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class VersionCapabilitiesTest {
    @Test
    void selectsLegacy1202Boundaries() {
        Map capabilities = VersionCapabilities.forMinecraft('1.20.2')

        assertFalse(capabilities.unobfuscatedMappings)
        assertFalse(capabilities.fabric.modernGameTests)
        assertTrue(capabilities.neoforge.legacyGradle)
        assertTrue(capabilities.neoforge.legacyMetadataFileName)
        assertTrue(capabilities.neoforge.legacyDependencyMetadata)
        assertTrue(capabilities.neoforge.legacyTickEvent)
        assertTrue(capabilities.neoforge.annotationGameTests)
        assertTrue(capabilities.neoforge.legacyStructureConverter)
    }

    @Test
    void separatesLegacyFileNamingFrom1202DependencySyntax() {
        Map capabilities = VersionCapabilities.forMinecraft('1.20.4')

        assertTrue(capabilities.neoforge.legacyGradle)
        assertTrue(capabilities.neoforge.legacyMetadataFileName)
        assertFalse(capabilities.neoforge.legacyDependencyMetadata)
    }

    @Test
    void selectsNewMetadataFilenameBeforeNeoGradleMigration() {
        Map capabilities = VersionCapabilities.forMinecraft('1.20.6')

        assertTrue(capabilities.neoforge.legacyGradle)
        assertFalse(capabilities.neoforge.legacyMetadataFileName)
        assertFalse(capabilities.neoforge.legacyDependencyMetadata)
    }

    @Test
    void selectsModern1215GameTestBoundaries() {
        Map capabilities = VersionCapabilities.forMinecraft('1.21.5')

        assertTrue(capabilities.fabric.modernGameTests)
        assertFalse(capabilities.neoforge.legacyGradle)
        assertTrue(capabilities.neoforge.eagerDataGameTests)
        assertTrue(capabilities.neoforge.explicitSubscriberBus)
    }

    @Test
    void selectsAnnotationGameTestsFor1211() {
        Map capabilities = VersionCapabilities.forMinecraft('1.21.1')

        assertTrue(capabilities.neoforge.annotationGameTests)
        assertFalse(capabilities.neoforge.eagerDataGameTests)
    }

    @Test
    void selectsIdentifierAndFml10Boundaries() {
        Map capabilities = VersionCapabilities.forMinecraft('1.21.11')

        assertTrue(capabilities.identifierNames)
        assertTrue(capabilities.neoforge.fml10)
        assertTrue(capabilities.neoforge.identifierDataGameTests)
    }

    @Test
    void selectsUnobfuscated26Boundaries() {
        Map capabilities = VersionCapabilities.forMinecraft('26.2')

        assertTrue(capabilities.unobfuscatedMappings)
        assertTrue(capabilities.identifierNames)
        assertTrue(capabilities.fabric.levelTickEvent)
        assertTrue(capabilities.fabric.modernGameTests)
        assertTrue(capabilities.neoforge.fml10)
        assertTrue(capabilities.neoforge.eagerDataGameTests)
    }

    @Test
    void selectsModernModIconMetadataFrom263() {
        assertTrue(VersionCapabilities.forMinecraft('26.3').neoforge.modernModIconMetadata)
        assertTrue(VersionCapabilities.forMinecraft('26.4').neoforge.modernModIconMetadata)
        assertFalse(VersionCapabilities.forMinecraft('26.2').neoforge.modernModIconMetadata)
        assertFalse(VersionCapabilities.forMinecraft('1.21.11').neoforge.modernModIconMetadata)
    }
}
