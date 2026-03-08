package dev.deacyde.gunturret;

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

public class GunTurretCommand extends AbstractPlayerCommand {

    public GunTurretCommand() {
        super("turret", "Gives you a Gun Turret item. Throw it to deploy.");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> playerRef,
                           @Nonnull PlayerRef player,
                           @Nonnull World world) {
        ItemStack turret = new ItemStack("Weapon_Deployable_Gun_Turret", 1);
        ItemUtils.throwItem(playerRef, turret, 0.0f, store);
        ctx.sendMessage(Message.raw("Gun Turret spawned! Throw it to deploy."));
    }
}
