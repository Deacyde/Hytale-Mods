package dev.deacyde.tankbarrel;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class TankBarrelCommand extends AbstractPlayerCommand {

    public enum Action { LOAD_TNT, LOAD_NUKE, FIRE_TNT, FIRE_NUKE, SHOW_AMMO }

    public TankBarrelCommand() {
        super("barrel", "Tank barrel commands. Usage: /barrel [give|load tnt|load nuke|fire tnt|fire nuke]");
        setAllowsExtraArguments(true);
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> playerRef,
                           @Nonnull PlayerRef player,
                           @Nonnull World world) {
        String raw = ctx.getInputString().trim();
        String[] tokens = raw.isEmpty() ? new String[0] : raw.split("\\s+");
        // tokens[0] is the command name ("barrel"), args start at index 1
        String[] args = tokens.length > 1 ? java.util.Arrays.copyOfRange(tokens, 1, tokens.length) : new String[0];

        if (args.length == 0 || args[0].equalsIgnoreCase("give")) {
            ItemStack barrel = new ItemStack("Weapon_Deployable_Tank_Barrel", 1);
            ItemUtils.throwItem(playerRef, barrel, 0.0f, store);
            ctx.sendMessage(Message.raw("§a[Barrel] Tank Barrel given! Throw it to deploy."));
            return;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("load") && args.length >= 2) {
            String ammoType = args[1].toLowerCase();
            if (ammoType.equals("tnt")) {
                TankBarrelPlugin.pendingActions.put(player.getUuid(), Action.LOAD_TNT);
                ctx.sendMessage(Message.raw("§e[Barrel] Loading TNT shells into nearest barrel..."));
            } else if (ammoType.equals("nuke")) {
                TankBarrelPlugin.pendingActions.put(player.getUuid(), Action.LOAD_NUKE);
                ctx.sendMessage(Message.raw("§e[Barrel] Loading Nuke shells into nearest barrel..."));
            } else {
                ctx.sendMessage(Message.raw("§c[Barrel] Unknown ammo. Use: /barrel load tnt|nuke"));
            }
            return;
        }

        if (sub.equals("fire")) {
            String ammoType = (args.length >= 2) ? args[1].toLowerCase() : "tnt";
            if (ammoType.equals("nuke")) {
                TankBarrelPlugin.pendingActions.put(player.getUuid(), Action.FIRE_NUKE);
                ctx.sendMessage(Message.raw("§c[Barrel] Firing Nuke shell!"));
            } else {
                TankBarrelPlugin.pendingActions.put(player.getUuid(), Action.FIRE_TNT);
                ctx.sendMessage(Message.raw("§e[Barrel] Firing TNT shell!"));
            }
            return;
        }

        if (sub.equals("ammo")) {
            // Show ammo status — ticking system will respond next tick
            TankBarrelPlugin.pendingActions.put(player.getUuid(), Action.SHOW_AMMO);
            return;
        }

        ctx.sendMessage(Message.raw("§7[Barrel] Usage: /barrel [give|load tnt|load nuke|fire tnt|fire nuke|ammo]"));
    }
}
