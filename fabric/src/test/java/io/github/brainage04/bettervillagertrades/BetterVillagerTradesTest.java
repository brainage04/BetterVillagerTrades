package io.github.brainage04.bettervillagertrades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.brainage04.bettervillagertrades.fabric.BetterVillagerTradesFabric;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BetterVillagerTradesTest {
    @BeforeAll
    static void bootstrapMinecraft() throws ClassNotFoundException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        new BetterVillagerTradesFabric().onInitialize();
    }

    @Test
    void registersCustomGameRulesWithEnabledDefaults() {
        GameRules rules = new GameRules(FeatureFlags.DEFAULT_FLAGS);
        assertTrue(rules.get(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES));
        assertTrue(rules.get(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES));
    }

    @Test
    void formatsTradeModificationLog() {
        assertEquals(
                "Modified enchants for Trade 2 with Novice Librarian Villager - minecraft:sharpness 1 -> minecraft:sharpness 5",
                BetterVillagerTrades.buildTradeModificationLog(
                        new BetterVillagerTrades.TradeContext(VillagerProfession.LIBRARIAN, 1, 2),
                        List.of(
                                new BetterVillagerTrades.EnchantmentChange(
                                        "minecraft:sharpness", 1, 5))));
    }

    @Test
    void formatsProfessionIdsWithEmptyWords() {
        ResourceKey<VillagerProfession> profession =
                ResourceKey.create(
                        Registries.VILLAGER_PROFESSION, Identifier.parse("test:_odd__name"));
        assertEquals(
                "Modified enchants for Trade 1 with Novice Odd Name Villager - minecraft:sharpness 1 -> minecraft:sharpness 5",
                BetterVillagerTrades.buildTradeModificationLog(
                        new BetterVillagerTrades.TradeContext(profession, 1, 1),
                        List.of(
                                new BetterVillagerTrades.EnchantmentChange(
                                        "minecraft:sharpness", 1, 5))));
    }

    @Test
    void keepsTradeFiltersIsolatedPerPlayer() {
        PlayerTradeFilters filters = new PlayerTradeFilters();
        TradeFilter mendingBook =
                new TradeFilter(
                        BuiltInRegistries.ITEM.getKey(Items.ENCHANTED_BOOK),
                        Optional.of(BetterVillagerTrades.of("mending")));
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();

        assertTrue(filters.add(firstPlayer, mendingBook));
        assertFalse(filters.add(firstPlayer, mendingBook));
        assertEquals(List.of(mendingBook), filters.get(firstPlayer));
        assertTrue(filters.get(secondPlayer).isEmpty());
        assertTrue(filters.remove(firstPlayer, mendingBook));
        assertTrue(filters.get(firstPlayer).isEmpty());
    }

    @Test
    void savedFiltersRoundTripWithoutSharingPlayerLists() {
        PlayerTradeFilters filters = new PlayerTradeFilters();
        UUID firstPlayer = new UUID(0, 1);
        UUID secondPlayer = new UUID(0, 2);
        TradeFilter books =
                new TradeFilter(
                        BuiltInRegistries.ITEM.getKey(Items.ENCHANTED_BOOK), Optional.empty());
        TradeFilter swords =
                new TradeFilter(
                        BuiltInRegistries.ITEM.getKey(Items.DIAMOND_SWORD), Optional.empty());
        filters.add(firstPlayer, books);
        filters.add(secondPlayer, swords);

        var encoded =
                PlayerTradeFilters.TYPE.codec().encodeStart(JsonOps.INSTANCE, filters).getOrThrow();
        PlayerTradeFilters restored =
                PlayerTradeFilters.TYPE.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(List.of(books), restored.get(firstPlayer));
        assertEquals(List.of(swords), restored.get(secondPlayer));
        assertTrue(restored.clear(firstPlayer));
        assertFalse(restored.clear(firstPlayer));
        assertEquals(List.of(swords), restored.get(secondPlayer));
        assertEquals(List.of(books), filters.get(firstPlayer));
    }

    @Test
    void savedFiltersKeepValidPlayersWhenAnotherKeyIsMalformed() {
        PlayerTradeFilters filters = new PlayerTradeFilters();
        UUID validPlayer = new UUID(0, 1);
        TradeFilter books =
                new TradeFilter(
                        BuiltInRegistries.ITEM.getKey(Items.ENCHANTED_BOOK), Optional.empty());
        filters.add(validPlayer, books);
        JsonObject encoded =
                PlayerTradeFilters.TYPE
                        .codec()
                        .encodeStart(JsonOps.INSTANCE, filters)
                        .getOrThrow()
                        .getAsJsonObject();
        JsonObject players = encoded.getAsJsonObject("filters");
        players.add("not-a-uuid", players.get(validPlayer.toString()).deepCopy());

        // SavedDataStorage.readSavedData keeps a partial parse result
        // (SavedDataStorage.java:93-95).
        PlayerTradeFilters restored =
                PlayerTradeFilters.TYPE
                        .codec()
                        .parse(JsonOps.INSTANCE, encoded)
                        .resultOrPartial()
                        .orElseThrow();
        assertEquals(List.of(books), restored.get(validPlayer));
    }
}
