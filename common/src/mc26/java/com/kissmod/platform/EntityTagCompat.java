package com.kissmod.platform;

import com.kissmod.Constants;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class EntityTagCompat {
  private static final Set<String> WARNED_INVALID_TAGS = new HashSet<>();

  public static boolean is(EntityType<?> entityType, String tagId) {
    Identifier location = parse(tagId);
    return location != null
        && entityType.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, location));
  }

  private static Identifier parse(String tagId) {
    if (tagId == null || tagId.isBlank()) {
      return null;
    }
    String normalized = tagId.startsWith("#") ? tagId.substring(1) : tagId;
    Identifier location = Identifier.tryParse(normalized);
    if (location == null && WARNED_INVALID_TAGS.add(tagId)) {
      Constants.LOG.warn("Ignoring invalid friendly entity tag '{}'", tagId);
    }
    return location;
  }

  private EntityTagCompat() {}
}
