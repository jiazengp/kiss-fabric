package com.kissmod.build

import groovy.json.JsonSlurper
import org.gradle.api.GradleException

import java.util.Properties

final class VersionManifest {
    static Map load(File rootFile) {
        Map root = (Map) new JsonSlurper().parse(rootFile)
        if (!(root.versions instanceof List) || root.versions.isEmpty()) {
            throw new GradleException("versions.json must contain a non-empty 'versions' list")
        }
        if (root.versions.toSet().size() != root.versions.size()) {
            throw new GradleException('versions.json must not contain duplicate version IDs')
        }

        File versionsDir = new File(rootFile.parentFile, 'versions')
        List<Map> versions = ((List<String>) root.versions).collect { String versionId ->
            File propsFile = new File(new File(versionsDir, versionId), 'gradle.properties')
            if (!propsFile.isFile()) {
                throw new GradleException("Missing version properties file '${propsFile}'")
            }
            Properties props = new Properties()
            propsFile.withInputStream { props.load(it) }
            String minecraftVersion = required(props, 'minecraft_version')
            Integer javaVersion = requiredInt(props, 'java_version')
            Integer expectedJavaVersion = minecraftVersion.startsWith('26.') ? 25 : 21
            if (javaVersion != expectedJavaVersion) {
                throw new GradleException(
                        "Version '${versionId}' must use java_version=${expectedJavaVersion} for minecraft_version=${minecraftVersion}")
            }
            Boolean fabricEnabled = requiredBoolean(props, 'enable_fabric')
            Boolean neoForgeEnabled = requiredBoolean(props, 'enable_neoforge')
            [
                    id                     : versionId,
                    minecraft_version      : minecraftVersion,
                    minecraft_version_range: required(props, 'minecraft_version_range'),
                    fabric_minecraft_version: string(props.getProperty('fabric_minecraft_version')),
                    java_version           : javaVersion,
                    neo_form_version       : string(props.getProperty('neo_form_version')),
                    parchment_minecraft    : string(props.getProperty('parchment_minecraft')),
                    parchment_version      : string(props.getProperty('parchment_version')),
                    capabilities           : VersionCapabilities.forMinecraft(minecraftVersion),
                    fabric                 : [
                            enabled              : fabricEnabled,
                            fabric_loader_version: loaderField(
                                    props, 'fabric_loader_version', 'fabric', versionId, fabricEnabled),
                            fabric_api_version   : loaderField(
                                    props, 'fabric_api_version', 'fabric', versionId, fabricEnabled),
                            fabric_permissions_api_version: loaderField(
                                    props, 'fabric_permissions_api_version', 'fabric', versionId, fabricEnabled)
                    ],
                    neoforge               : [
                            enabled                      : neoForgeEnabled,
                            neoforge_version             : loaderField(
                                    props, 'neoforge_version', 'neoforge', versionId, neoForgeEnabled),
                            neoforge_loader_version_range: loaderField(
                                    props, 'neoforge_loader_version_range', 'neoforge', versionId, neoForgeEnabled)
                    ]
            ]
        }
        return [versions: versions]
    }

    static Map findById(Map manifest, String id) {
        manifest.versions.find { it.id == id } as Map
    }

    static List<String> enabledLoaders(Map version) {
        ['fabric', 'neoforge'].findAll { String loader ->
            ((Map) version[loader]).enabled
        }
    }

    static String projectPath(String versionId, String loader) {
        ":${versionId}:${loader}"
    }

    private static String required(Properties source, String key) {
        String value = source.getProperty(key)
        if (value == null || value.trim().isEmpty()) {
            throw new GradleException("Missing required version field '${key}'")
        }
        value
    }

    private static Integer requiredInt(Properties source, String key) {
        try {
            return Integer.parseInt(required(source, key))
        } catch (NumberFormatException ex) {
            throw new GradleException("Invalid integer value for version field '${key}'", ex)
        }
    }

    private static Boolean requiredBoolean(Properties source, String key) {
        String value = required(source, key)
        if (!['true', 'false'].contains(value)) {
            throw new GradleException("Invalid boolean value for version field '${key}'")
        }
        Boolean.parseBoolean(value)
    }

    private static String loaderField(
            Properties source, String key, String loader, String versionId, boolean enabled) {
        String value = string(source.getProperty(key))
        if (enabled && value.trim().isEmpty()) {
            throw new GradleException(
                    "Enabled loader '${loader}' in version '${versionId}' is missing '${key}'")
        }
        value
    }

    private static String string(Object value) {
        value == null ? '' : value.toString()
    }
}
