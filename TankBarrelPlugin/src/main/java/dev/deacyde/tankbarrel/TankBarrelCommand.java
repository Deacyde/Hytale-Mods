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
            // Determine facing direction: explicit arg or snap from player yaw
            String dir = "N";
            if (args.length >= 2) {
                dir = args[1].toUpperCase();
                if (!dir.equals("N") && !dir.equals("E") && !dir.equals("S") && !dir.equals("W")) {
                    ctx.sendMessage(Message.raw("§c[Barrel] Unknown direction. Use: n, e, s, w"));
                    return;
                }
            } else {
                // Snap player's head yaw to nearest cardinal direction
                // Hytale yaw: 0=North, 90=East, 180=South, 270=West (all in degrees)
                float yaw = player.getHeadRotation().getYaw();
                // Normalize to [0, 360)
                yaw = ((yaw % 360) + 360) % 360;
                if (yaw >= 315 || yaw < 45)       dir = "N";
                else if (yaw >= 45 && yaw < 135)  dir = "E";
                else if (yaw >= 135 && yaw < 225) dir = "S";
                else                               dir = "W";
            }
            String itemId = dir.equals("N") ? "Weapon_Deployable_Tank_Barrel"
                                            : "Weapon_Deployable_Tank_Barrel_" + dir;
            ItemStack barrel = new ItemStack(itemId, 1);
            ItemUtils.throwItem(playerRef, barrel, 0.0f, store);
            String[] names = {"N=North", "E=East", "S=South", "W=West"};
            java.util.Map<String,String> dirNames = new java.util.HashMap<>();
            dirNames.put("N", "North"); dirNames.put("E", "East");
            dirNames.put("S", "South"); dirNames.put("W", "West");
            ctx.sendMessage(Message.raw("§a[Barrel] Tank Barrel (" + dirNames.get(dir) + ") given! Throw to deploy. Use §7/barrel give [n|e|s|w]§a to pick direction."));
            return;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("load") && args.length >= 2) {
            String ammoType = args[1].toLowerCase();
            if (ammoType.equals("tnt")) {
                TankBarrelPlugin.pendingActions.put(player.getUuid(), new TankBarrelPlugin.PendingAction(Action.LOAD_TNT, player));
                ctx.sendMessage(Message.raw("§e[Barrel] Loading TNT shells into nearest barrel..."));
            } else if (ammoType.equals("nuke")) {
                TankBarrelPlugin.pendingActions.put(player.getUuid(), new TankBarrelPlugin.PendingAction(Action.LOAD_NUKE, player));
                ctx.sendMessage(Message.raw("§e[Barrel] Loading Nuke shells into nearest barrel..."));
            } else {
                ctx.sendMessage(Message.raw("§c[Barrel] Unknown ammo. Use: /barrel load tnt|nuke"));
            }
            return;
        }

        if (sub.equals("fire")) {
            String ammoType = (args.length >= 2) ? args[1].toLowerCase() : "tnt";
            if (ammoType.equals("nuke")) {
                TankBarrelPlugin.pendingActions.put(player.getUuid(), new TankBarrelPlugin.PendingAction(Action.FIRE_NUKE, player));
                ctx.sendMessage(Message.raw("§c[Barrel] Firing Nuke shell!"));
            } else {
                TankBarrelPlugin.pendingActions.put(player.getUuid(), new TankBarrelPlugin.PendingAction(Action.FIRE_TNT, player));
                ctx.sendMessage(Message.raw("§e[Barrel] Firing TNT shell!"));
            }
            return;
        }

        if (sub.equals("ammo")) {
            TankBarrelPlugin.pendingActions.put(player.getUuid(), new TankBarrelPlugin.PendingAction(Action.SHOW_AMMO, player));
            return;
        }

        ctx.sendMessage(Message.raw("§7[Barrel] Usage: /barrel [give [n|e|s|w]|load tnt|load nuke|fire tnt|fire nuke|ammo]"));
    }
}
