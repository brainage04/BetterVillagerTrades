package io.github.brainage04.bettervillagertrades;

import io.github.brainage04.minecraftmodconventions.ClientGameTestRecorder;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * The client GameTest's film: a librarian and a weaponsmith whose generated trades are maxed, their
 * results staged in the hotbar, and the captions around the trading screen.
 */
@SuppressWarnings("UnstableApiUsage")
public final class BetterVillagerTradesShowcase {
    private static UUID librarianId;
    private static UUID equipmentSmithId;

    private BetterVillagerTradesShowcase() {}

    /** The staged librarian, whose trading screen the GameTest opens. */
    public static UUID librarianId() {
        return librarianId;
    }

    public static void stage(MinecraftServer server) {
        ServerLevel level = server.overworld();
        server.getGameRules().set(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES, true, server);
        server.getGameRules().set(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES, true, server);
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        player.teleportTo(level, 2.5, -60.0, 4.0, Set.of(), 180.0F, 0.0F, false);

        Villager librarian =
                generatedVillager(level, VillagerProfession.LIBRARIAN, 1, true, 2.5, -60.0, 2.5);
        Villager equipmentSmith =
                generatedVillager(level, VillagerProfession.WEAPONSMITH, 5, false, 5.5, -60.0, 2.5);
        player.getInventory().setItem(0, firstEnchantedTrade(librarian, true));
        player.getInventory().setItem(1, firstEnchantedTrade(equipmentSmith, false));
        librarianId = librarian.getUUID();
        equipmentSmithId = equipmentSmith.getUUID();
    }

    public static void showMaxBook(ClientGameTestContext context) {
        ClientGameTestRecorder.startRecording(context);
        ClientGameTestRecorder.showStep(
                context,
                "librarian-max-book",
                "Generated max-level enchanted book",
                "A real librarian offer result is staged in the visible inventory with every enchantment at its maximum level");
        openInventoryScreen(context);
        context.waitTicks(60);
    }

    public static void captionRerollControls(ClientGameTestContext context) {
        ClientGameTestRecorder.showStep(
                context,
                "trade-reroll-controls",
                "Per-player reroll controls",
                "The optional client adds Reroll trades and Toggle filter controls beneath the standard villager trading screen");
    }

    public static void showMaxEquipment(ClientGameTestContext context) {
        context.waitTicks(60);
        ClientGameTestRecorder.showStep(
                context,
                "equipment-max-item",
                "Generated max-level equipment",
                "A real weaponsmith offer result is staged in the visible inventory with every enchantment at its maximum level");
        openInventoryScreen(context);
        context.waitTicks(60);
    }

    public static void cleanup(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.closeContainer();
            PlayerTradeFilters.get(server).clear(player);
        }
        if (librarianId != null) {
            Entity librarian = level.getEntity(librarianId);
            if (librarian != null) librarian.discard();
        }
        if (equipmentSmithId != null) {
            Entity equipmentSmith = level.getEntity(equipmentSmithId);
            if (equipmentSmith != null) equipmentSmith.discard();
        }
        librarianId = null;
        equipmentSmithId = null;
    }

    private static Villager generatedVillager(
            ServerLevel level,
            ResourceKey<VillagerProfession> profession,
            int professionLevel,
            boolean bookTrade,
            double x,
            double y,
            double z) {
        for (int attempt = 0; attempt < 40; attempt++) {
            Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
            if (villager == null)
                throw new AssertionError("Expected villager creation to succeed.");
            villager.setPos(x, y, z);
            villager.setNoAi(true);
            villager.getRandom().setSeed(100L + attempt);
            villager.setVillagerData(
                    villager.getVillagerData()
                            .withProfession(level.registryAccess(), profession)
                            .withLevel(professionLevel));
            level.addFreshEntity(villager);
            if (hasMaxedGeneratedTrade(villager, bookTrade)) return villager;
            villager.discard();
        }
        throw new AssertionError(
                "Expected a generated "
                        + (bookTrade ? "librarian book" : "equipment")
                        + " trade with enchantments.");
    }

    private static boolean hasMaxedGeneratedTrade(Villager villager, boolean bookTrade) {
        for (MerchantOffer offer : villager.getOffers()) {
            ItemEnchantments enchantments =
                    offer.getResult()
                            .getOrDefault(enchantmentComponent(bookTrade), ItemEnchantments.EMPTY);
            if (!enchantments.isEmpty()) {
                for (Holder<Enchantment> enchantment : enchantments.keySet()) {
                    if (enchantments.getLevel(enchantment) != enchantment.value().getMaxLevel())
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    private static ItemStack firstEnchantedTrade(Villager villager, boolean bookTrade) {
        for (MerchantOffer offer : villager.getOffers()) {
            if (!offer.getResult()
                    .getOrDefault(enchantmentComponent(bookTrade), ItemEnchantments.EMPTY)
                    .isEmpty()) return offer.getResult().copy();
        }
        throw new AssertionError("Expected a generated enchanted trade result.");
    }

    private static DataComponentType<ItemEnchantments> enchantmentComponent(boolean bookTrade) {
        return bookTrade ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
    }

    private static void openInventoryScreen(ClientGameTestContext context) {
        context.runOnClient(
                client -> {
                    if (client.player.containerMenu instanceof MerchantMenu) {
                        client.player.closeContainer();
                    }
                    client.gui.setScreen(new InventoryScreen(client.player));
                });
    }
}
