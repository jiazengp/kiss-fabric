package com.kissmod.gametest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;

public final class GameTestStructureConverter {
  private GameTestStructureConverter() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException("Expected an SNBT input path and an NBT output path");
    }

    Path source = Path.of(args[0]);
    Path destination = Path.of(args[1]);
    Files.createDirectories(Objects.requireNonNull(destination.getParent()));
    NbtIo.writeCompressed(NbtUtils.snbtToStructure(Files.readString(source)), destination.toFile());
  }
}
