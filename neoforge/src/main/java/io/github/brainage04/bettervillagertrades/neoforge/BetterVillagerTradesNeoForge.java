package io.github.brainage04.bettervillagertrades.neoforge;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import io.github.brainage04.bettervillagertrades.BetterVillagerTrades;
import io.github.brainage04.bettervillagertrades.BetterVillagerTradesCommands;
import io.github.brainage04.bettervillagertrades.TradeRerollService;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BetterVillagerTrades.MOD_ID)
public final class BetterVillagerTradesNeoForge {
    private static final DeferredRegister<GameRule<?>> GAME_RULES =
            DeferredRegister.create(Registries.GAME_RULE, BetterVillagerTrades.MOD_ID);
    private static final Map<
                    BetterVillagerTrades.BooleanRule,
                    DeferredHolder<GameRule<?>, GameRule<Boolean>>>
            GAME_RULE_HOLDERS =
                    BetterVillagerTrades.GAME_RULES.stream()
                            .collect(
                                    Collectors.toUnmodifiableMap(
                                            Function.identity(),
                                            BetterVillagerTradesNeoForge::registerBoolean));

    public BetterVillagerTradesNeoForge(IEventBus modEventBus) {
        GAME_RULES.register(modEventBus);
        modEventBus.addListener(
                (FMLCommonSetupEvent event) ->
                        BetterVillagerTrades.initialize(rule -> GAME_RULE_HOLDERS.get(rule).get()));
        NeoForge.EVENT_BUS.addListener(
                (RegisterCommandsEvent event) ->
                        BetterVillagerTradesCommands.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        InteractionResult result =
                TradeRerollService.interact(event.getEntity(), event.getTarget(), event.getHand());
        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static DeferredHolder<GameRule<?>, GameRule<Boolean>> registerBoolean(
            BetterVillagerTrades.BooleanRule rule) {
        return GAME_RULES.register(
                rule.name(),
                () ->
                        new GameRule<>(
                                GameRuleCategory.MISC,
                                GameRuleType.BOOL,
                                BoolArgumentType.bool(),
                                (visitor, gameRule) -> visitor.visitBoolean(gameRule),
                                Codec.BOOL,
                                value -> value ? 1 : 0,
                                rule.defaultValue(),
                                FeatureFlagSet.of()));
    }
}
