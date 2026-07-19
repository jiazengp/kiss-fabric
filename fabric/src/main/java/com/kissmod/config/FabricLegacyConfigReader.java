package com.kissmod.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

/** Reads the JSON configuration written by Fabric releases of KissMod v1. */
final class FabricLegacyConfigReader {

  private FabricLegacyConfigReader() {}

  static KissConfigData load(Path path) throws IOException {
    Object loaded;
    try (InputStream input = Files.newInputStream(path)) {
      loaded = new Load(LoadSettings.builder().build()).loadFromInputStream(input);
    }
    if (!(loaded instanceof Map<?, ?> values)) {
      throw new IllegalArgumentException("Legacy configuration root must be an object");
    }
    return LegacyKissConfigMapper.mergeWithDefaults(values);
  }
}
