package com.kissmod.build

/**
 * Named Minecraft API boundaries used by the shared build.
 *
 * Keep exact-version exceptions here instead of spreading version checks across loader build
 * scripts. These flags describe APIs, not release support; loader enablement remains in the
 * version manifest.
 */
final class VersionCapabilities {
    private static final Set<String> LEGACY_FABRIC_GAMETESTS = [
            '1.20.1', '1.20.2', '1.20.4', '1.20.6',
            '1.21.1', '1.21.3', '1.21.4'
    ] as Set
    private static final Set<String> LEGACY_NEOFORGE_TICK = ['1.20.2', '1.20.4'] as Set
    private static final Set<String> ANNOTATION_NEOFORGE_GAMETESTS = [
            '1.20.2', '1.20.4', '1.20.6', '1.21.1', '1.21.3', '1.21.4'
    ] as Set
    private static final Set<String> EAGER_NEOFORGE_DATA_GAMETESTS = [
            '1.21.5', '1.21.8', '1.21.10', '1.21.11'
    ] as Set

    static Map forMinecraft(String minecraftVersion) {
        boolean minecraft26 = minecraftVersion.startsWith('26.')
        boolean identifierNames = minecraftVersion == '1.21.11' || minecraft26

        [
                unobfuscatedMappings: minecraft26,
                identifierNames     : identifierNames,
                fabric              : [
                        levelTickEvent: minecraft26,
                        modernGameTests: !LEGACY_FABRIC_GAMETESTS.contains(minecraftVersion)
                ].asImmutable(),
                neoforge            : [
                        legacyGradle             : minecraftVersion.startsWith('1.20.'),
                        legacyMetadataFileName   : minecraftVersion in ['1.20.2', '1.20.4'],
                        legacyDependencyMetadata : minecraftVersion == '1.20.2',
                        legacyTickEvent          : LEGACY_NEOFORGE_TICK.contains(minecraftVersion),
                        fml10                    : minecraftVersion in ['1.21.10', '1.21.11'] || minecraft26,
                        annotationGameTests      : ANNOTATION_NEOFORGE_GAMETESTS.contains(minecraftVersion),
                        eagerDataGameTests       : EAGER_NEOFORGE_DATA_GAMETESTS.contains(minecraftVersion) || minecraft26,
                        identifierDataGameTests  : identifierNames,
                        explicitSubscriberBus   : minecraftVersion == '1.21.5',
                        legacyStructureConverter: minecraftVersion == '1.20.2'
                ].asImmutable()
        ].asImmutable()
    }

    private VersionCapabilities() {}
}
