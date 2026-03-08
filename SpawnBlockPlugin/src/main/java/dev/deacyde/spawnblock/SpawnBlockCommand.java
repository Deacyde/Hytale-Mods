package dev.deacyde.spawnblock;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.io.PrintWriter;
import java.io.FileWriter;

/**
 * /spawnblock give                              — gives the spawn block item
 * /spawnblock <mob> [rateSecs] [maxMobs] [radius] — stores pending config
 */
public class SpawnBlockCommand extends AbstractPlayerCommand {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Try both forms — the namespaced ID is what Hytale uses for plugin items
    private static final String BLOCK_ITEM_ID_PLAIN = "Block_Spawn_Block";
    private static final String BLOCK_ITEM_ID_NS    = "Deacyde:Block_Spawn_Block";

    private static final String[] MOB_EXAMPLES =
        {"Goblin", "Trork", "Skeleton", "Zombie", "Wolf", "Emberwulf", "Spider", "Bear_Grizzly"};

    public SpawnBlockCommand() {
        super("spawnblock", "SpawnBlock: /spawnblock give | /spawnblock <mob> [rateSecs] [maxMobs] [radius]");
        setAllowsExtraArguments(true);
        try (PrintWriter pw = new PrintWriter(new FileWriter("/tmp/spawnblock_diag.txt", true))) {
            pw.println("SpawnBlockCommand constructor called");
        } catch (Exception e) { /* ignore */ }
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> playerRef,
                           @Nonnull PlayerRef player,
                           @Nonnull World world) {
        // FILE DIAGNOSTIC: bypass all server logging to prove execute() is called
        try (PrintWriter pw = new PrintWriter(new FileWriter("/tmp/spawnblock_diag.txt", true))) {
            pw.println("execute() called! input=" + ctx.getInputString()
                + " thread=" + Thread.currentThread().getName());
        } catch (Exception fileEx) { /* ignore */ }
        try {
            executeInner(ctx, store, playerRef, player, world);
        } catch (Throwable t) {
            System.out.println("[SpawnBlock] COMMAND ERROR: " + t);
            t.printStackTrace();
            LOGGER.atSevere().log("[SpawnBlock] Command exception: " + t.getMessage());
            ctx.sendMessage(Message.raw("§c[SpawnBlock] Error: " + t.getMessage()));
        }
    }

    private void executeInner(@Nonnull CommandContext ctx,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull Ref<EntityStore> playerRef,
                              @Nonnull PlayerRef player,
                              @Nonnull World world) {
        String raw = ctx.getInputString().trim();
        String[] tokens = raw.isEmpty() ? new String[0] : raw.split("\\s+");
        // tokens[0] is the command name ("spawnblock") — skip it
        String[] args = tokens.length > 1
            ? java.util.Arrays.copyOfRange(tokens, 1, tokens.length)
            : new String[0];

        if (args.length == 0) {
            sendHelp(ctx);
            return;
        }

        // /spawnblock give — hand the player a spawn block
        if (args[0].equalsIgnoreCase("give")) {
            Player playerEntity = (Player) store.getComponent(playerRef, Player.getComponentType());
            if (playerEntity == null) {
                LOGGER.atWarning().log("[SpawnBlock] give: playerEntity is NULL");
                ctx.sendMessage(Message.raw("§c[SpawnBlock] Could not find player entity."));
                return;
            }
            // Give both forms so we can see which one works
            LOGGER.atInfo().log("[SpawnBlock] Giving item IDs: " + BLOCK_ITEM_ID_PLAIN + " and " + BLOCK_ITEM_ID_NS);
            var tx1 = playerEntity.giveItem(new ItemStack(BLOCK_ITEM_ID_PLAIN, 1), playerRef, store);
            var tx2 = playerEntity.giveItem(new ItemStack(BLOCK_ITEM_ID_NS, 1), playerRef, store);
            LOGGER.atInfo().log("[SpawnBlock] Give result — plain remainder=" +
                (tx1 != null ? tx1.getRemainder() : "null") +
                " ns remainder=" + (tx2 != null ? tx2.getRemainder() : "null"));
            ctx.sendMessage(Message.raw("§a[SpawnBlock] Given spawn block items. Check which one is placeable!"));
            return;
        }

        // /spawnblock <mob> [rate] [max] [radius] — store pending config
        String mobRole = capitalize(args[0]);
        int rateSecs = args.length > 1 ? parseInt(args[1], 5)  : 5;
        int maxMobs  = args.length > 2 ? parseInt(args[2], 5)  : 5;
        int radius   = args.length > 3 ? parseInt(args[3], 10) : 10;

        rateSecs = Math.max(1, Math.min(rateSecs, 300));
        maxMobs  = Math.max(1, Math.min(maxMobs,  50));
        radius   = Math.max(1, Math.min(radius,   100));

        SpawnBlockPlugin.get().getRegistry().storePending(mobRole, rateSecs, maxMobs, radius);

        ctx.sendMessage(Message.raw(
            "§a[SpawnBlock] Config saved! §eMob: §f" + mobRole +
            " §eEvery: §f" + rateSecs + "s §eMax: §f" + maxMobs +
            " §eRadius: §f" + radius
        ));
        ctx.sendMessage(Message.raw("§7Place a §fSpawn Block§7 to activate. (Use §f/spawnblock give§7 to get one.)"));
    }

    private void sendHelp(CommandContext ctx) {
        ctx.sendMessage(Message.raw("§6/spawnblock give §f— get a spawn block item"));
        ctx.sendMessage(Message.raw("§6/spawnblock <mob> [rate] [max] [radius] §f— configure spawning"));
        ctx.sendMessage(Message.raw("§7  Example: §f/spawnblock Goblin 3 5 10"));
        ctx.sendMessage(Message.raw("§7  Mobs: §f" + String.join(", ", MOB_EXAMPLES)));
    }

    private static int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

