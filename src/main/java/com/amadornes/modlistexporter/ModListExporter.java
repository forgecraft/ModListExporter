package com.amadornes.modlistexporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.i18n.MavenVersionTranslator;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

@Mod(ModListExporter.MODID)
public class ModListExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModListExporter.class);

    public static final String MODID = "modlistexporter";

    private static final Gson GSON = new GsonBuilder()
            .setLenient()
            .setPrettyPrinting()
            .create();

    public ModListExporter(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::commonSetup);

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        new Thread(this::offThread, "Mod List Writer").start();
    }

    private void offThread() {
        try {
            var modList = gatherModList();
            writeModList(modList);
        } catch (IOException e) {
            LOGGER.error("Failed to write mod list", e);
            throw new RuntimeException(e);
        }
    }

    private JsonModList gatherModList() {
        return new JsonModList(ModList.get().getMods().stream()
                .filter(m -> !Config.hiddenModIds.contains(m.getModId()))
                .map(JsonModList.ModInfo::new)
                .sorted(Comparator.comparing(JsonModList.ModInfo::name, String::compareToIgnoreCase))
                .toList());
    }

    private void writeModList(JsonModList modList) throws IOException {
        var tmp = FMLPaths.GAMEDIR.get().resolve(UUID.randomUUID().toString().toLowerCase(Locale.ROOT).replace("-", "") + ".json");
        try (var writer = Files.newBufferedWriter(tmp, WRITE, CREATE)) {
            GSON.toJson(modList, writer);
        }
        var options = new ArrayList<>(List.of(REPLACE_EXISTING));
        if (Config.atomicMove) {
            options.add(ATOMIC_MOVE);
        }
        Files.move(tmp, Path.of(Config.outputPath), options.toArray(CopyOption[]::new));
    }

    public record JsonModList(List<ModInfo> mods) {

        public record ModInfo(String id, String name, String version, String summary) {

            public ModInfo(IModInfo mod) {
                this(
                        mod.getModId(),
                        mod.getDisplayName(),
                        MavenVersionTranslator.artifactVersionToString(mod.getVersion()),
                        mod.getDescription().trim()
                );
            }

        }

    }

}
