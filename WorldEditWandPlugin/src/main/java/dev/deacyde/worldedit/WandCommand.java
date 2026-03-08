package dev.deacyde.worldedit;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * /we <subcommand> [args...]
 *
 *  wand              — give WorldEdit Wand item
 *  pos1              — set pos1 at player's feet
 *  pos2              — set pos2 at player's feet
 *  set <blockId>     — fill selection with blockId
 *  walls <blockId>   — fill only the 4 side walls of the selection
 *  clear             — fill selection with air
 *  copy              — copy selection to clipboard (origin = pos1 corner)
 *  paste             — paste clipboard at player's feet
 *  undo              — undo last fill/paste
 *  size              — show selection dimensions
 */
public class WandCommand extends AbstractPlayerCommand {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final WorldEditPlugin plugin;

    public WandCommand(WorldEditPlugin plugin) {
        super("we", "WorldEdit wand — /we <wand|pos1|pos2|set|walls|clear|copy|paste|undo|size>");
        setAllowsExtraArguments(true);
        this.plugin = plugin;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> playerRef,
                           @Nonnull PlayerRef player,
                           @Nonnull World world) {
        try {
            executeInner(ctx, store, playerRef, player, world);
        } catch (Throwable t) {
            LOGGER.atSevere().log("[WorldEdit] Command error: " + t);
            ctx.sendMessage(Message.raw("§c[WE] Error: " + t.getMessage()));
        }
    }

    private void executeInner(@Nonnull CommandContext ctx,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull Ref<EntityStore> playerRef,
                              @Nonnull PlayerRef player,
                              @Nonnull World world) {
        String raw = ctx.getInputString().trim();
        String[] tokens = raw.isEmpty() ? new String[0] : raw.split("\\s+");
        // tokens[0] = "we", skip it
        String[] args = tokens.length > 1 ? java.util.Arrays.copyOfRange(tokens, 1, tokens.length) : new String[0];

        if (args.length == 0) {
            sendHelp(ctx);
            return;
        }

        WandSession session = plugin.getSession(player.getUuid());
        LOGGER.atInfo().log("[WorldEdit] /we " + args[0] + " uuid=" + player.getUuid()
            + " pos1=" + session.pos1 + " pos2=" + session.pos2
            + " hasSelection=" + session.hasSelection());

        switch (args[0].toLowerCase()) {
            case "wand" -> giveWand(ctx, store, playerRef, player);
            case "pos1" -> setPos1AtFeet(ctx, session, player);
            case "pos2" -> setPos2AtFeet(ctx, session, player);
            case "set"  -> {
                if (args.length < 2) { ctx.sendMessage(Message.raw("§c[WE] Usage: /we set <blockId>")); return; }
                doFill(ctx, session, world, args[1], false);
            }
            case "walls" -> {
                if (args.length < 2) { ctx.sendMessage(Message.raw("§c[WE] Usage: /we walls <blockId>")); return; }
                doWalls(ctx, session, world, args[1]);
            }
            case "clear" -> doFill(ctx, session, world, BlockType.EMPTY_KEY, false);
            case "copy"  -> doCopy(ctx, session, world, player);
            case "paste" -> doPaste(ctx, session, world, player);
            case "undo"  -> doUndo(ctx, session, world);
            case "size"  -> doSize(ctx, session);
            default -> {
                ctx.sendMessage(Message.raw("§c[WE] Unknown subcommand: " + args[0]));
                sendHelp(ctx);
            }
        }
    }

    // ──────────────────────── wand give ────────────────────────

    private void giveWand(@Nonnull CommandContext ctx,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> playerRef,
                          @Nonnull PlayerRef player) {
        Player playerEntity = (Player) store.getComponent(playerRef, Player.getComponentType());
        if (playerEntity == null) {
            ctx.sendMessage(Message.raw("§c[WE] Could not find player entity."));
            return;
        }
        ItemUtils.throwItem(playerRef, new ItemStack(WorldEditPlugin.WAND_ITEM_ID, 1), 0.0f, store);
        ctx.sendMessage(Message.raw("§a[WE] WorldEdit Wand given! Left-click=Pos1, Right-click=Pos2."));
    }

    // ──────────────────────── pos1/pos2 ────────────────────────

    private void setPos1AtFeet(CommandContext ctx, WandSession session, PlayerRef player) {
        Vector3d pos = player.getTransform().getPosition();
        session.pos1 = new Vector3i((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
        session.nextClickIsPos2 = true;   // next wand click should set pos2
        LOGGER.atInfo().log("[WorldEdit] pos1 set at feet: " + session.pos1 + " uuid=" + player.getUuid());
        ctx.sendMessage(Message.raw("§a[WE] Pos1 set to §f(" + session.pos1.x + ", " + session.pos1.y + ", " + session.pos1.z + ")"));
    }

    private void setPos2AtFeet(CommandContext ctx, WandSession session, PlayerRef player) {
        Vector3d pos = player.getTransform().getPosition();
        session.pos2 = new Vector3i((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
        session.nextClickIsPos2 = false;  // next wand click should set pos1
        LOGGER.atInfo().log("[WorldEdit] pos2 set at feet: " + session.pos2 + " uuid=" + player.getUuid());
        ctx.sendMessage(Message.raw("§b[WE] Pos2 set to §f(" + session.pos2.x + ", " + session.pos2.y + ", " + session.pos2.z + ")"));
    }

    // ──────────────────────── fill ────────────────────────

    private void doFill(CommandContext ctx, WandSession session, World world, String blockId, boolean wallsOnly) {
        LOGGER.atInfo().log("[WorldEdit] doFill entry: blockId=" + blockId
            + " pos1=" + session.pos1 + " pos2=" + session.pos2
            + " hasSelection=" + session.hasSelection());
        if (!session.hasSelection()) {
            ctx.sendMessage(Message.raw("§c[WE] No selection — use the wand or /we pos1 /we pos2 first."));
            return;
        }
        long total = session.getTotalBlocks();
        if (total > WorldEditPlugin.MAX_BLOCKS) {
            ctx.sendMessage(Message.raw("§c[WE] Selection too large (" + total + " blocks). Max: " + WorldEditPlugin.MAX_BLOCKS));
            return;
        }

        Vector3i min = session.getMin();
        Vector3i max = session.getMax();
        int sx = session.getSizeX(), sy = session.getSizeY(), sz = session.getSizeZ();

        LOGGER.atInfo().log("[WorldEdit] doFill: blockId=" + blockId
            + " pos1=" + session.pos1 + " pos2=" + session.pos2
            + " min=" + min + " max=" + max + " size=" + sx + "x" + sy + "x" + sz);

        // Save undo buffer before modifying (skip for large selections — too slow to pre-read)
        if (total <= WorldEditPlugin.UNDO_SKIP_THRESHOLD) {
            String[][][] undo = new String[sx][sy][sz];
            for (int x = min.x; x <= max.x; x++)
                for (int y = min.y; y <= max.y; y++)
                    for (int z = min.z; z <= max.z; z++) {
                        BlockType bt = world.getBlockType(x, y, z);
                        undo[x - min.x][y - min.y][z - min.z] = (bt != null) ? bt.getId() : BlockType.EMPTY_KEY;
                    }
            session.undoBuffer = undo;
            session.undoOrigin = min;
        } else {
            session.undoBuffer = null;
            session.undoOrigin = null;
        }

        // Fill — use integer block ID for Empty to avoid string lookup issues
        boolean isClearing = BlockType.EMPTY_KEY.equals(blockId);
        int count = 0;
        try {
            for (int x = min.x; x <= max.x; x++) {
                for (int y = min.y; y <= max.y; y++) {
                    for (int z = min.z; z <= max.z; z++) {
                        if (wallsOnly) {
                            boolean onWall = x == min.x || x == max.x || z == min.z || z == max.z;
                            if (!onWall) continue;
                        }
                        if (isClearing) {
                            world.setBlock(x, y, z, BlockType.EMPTY_KEY);
                        } else {
                            world.setBlock(x, y, z, blockId);
                        }
                        count++;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            ctx.sendMessage(Message.raw("§c[WE] Unknown block ID: §f" + blockId
                + "§c. Use Hytale IDs e.g. §fRock_Stone§c, §fSoil_Dirt§c, §fWood_Pine_Log§c, §fBrick_Cut"));
            return;
        }
        ctx.sendMessage(Message.raw("§a[WE] §e" + count + "§a blocks set to §f" + (isClearing ? "air" : blockId)));
    }

    private void doWalls(CommandContext ctx, WandSession session, World world, String blockId) {
        if (!session.hasSelection()) {
            ctx.sendMessage(Message.raw("§c[WE] No selection."));
            return;
        }
        long total = session.getTotalBlocks();
        if (total > WorldEditPlugin.MAX_BLOCKS) {
            ctx.sendMessage(Message.raw("§c[WE] Selection too large (" + total + " blocks). Max: " + WorldEditPlugin.MAX_BLOCKS));
            return;
        }

        Vector3i min = session.getMin();
        Vector3i max = session.getMax();
        int sx = session.getSizeX(), sy = session.getSizeY(), sz = session.getSizeZ();

        // Undo buffer
        String[][][] undo = new String[sx][sy][sz];
        for (int x = min.x; x <= max.x; x++)
            for (int y = min.y; y <= max.y; y++)
                for (int z = min.z; z <= max.z; z++) {
                    BlockType bt = world.getBlockType(x, y, z);
                    undo[x - min.x][y - min.y][z - min.z] = (bt != null) ? bt.getId() : BlockType.EMPTY_KEY;
                }
        session.undoBuffer = undo;
        session.undoOrigin = min;

        int count = 0;
        try {
            for (int x = min.x; x <= max.x; x++) {
                for (int y = min.y; y <= max.y; y++) {
                    for (int z = min.z; z <= max.z; z++) {
                        boolean onWall = x == min.x || x == max.x || z == min.z || z == max.z;
                        if (onWall) {
                            world.setBlock(x, y, z, blockId);
                            count++;
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            ctx.sendMessage(Message.raw("§c[WE] Unknown block ID: §f" + blockId
                + "§c. Use Hytale IDs e.g. §fRock_Stone§c, §fSoil_Dirt§c, §fWood_Pine_Log§c, §fBrick_Cut"));
            return;
        }
        ctx.sendMessage(Message.raw("§a[WE] §e" + count + "§a wall blocks set to §f" + blockId));
    }

    // ──────────────────────── copy ────────────────────────

    private void doCopy(CommandContext ctx, WandSession session, World world, PlayerRef player) {
        if (!session.hasSelection()) {
            ctx.sendMessage(Message.raw("§c[WE] No selection."));
            return;
        }
        long total = session.getTotalBlocks();
        if (total > WorldEditPlugin.MAX_BLOCKS) {
            ctx.sendMessage(Message.raw("§c[WE] Selection too large (" + total + " blocks). Max: " + WorldEditPlugin.MAX_BLOCKS));
            return;
        }

        Vector3i min = session.getMin();
        Vector3i max = session.getMax();
        int sx = session.getSizeX(), sy = session.getSizeY(), sz = session.getSizeZ();

        String[][][] clip = new String[sx][sy][sz];
        for (int x = min.x; x <= max.x; x++)
            for (int y = min.y; y <= max.y; y++)
                for (int z = min.z; z <= max.z; z++) {
                    BlockType bt = world.getBlockType(x, y, z);
                    clip[x - min.x][y - min.y][z - min.z] = (bt != null) ? bt.getId() : BlockType.EMPTY_KEY;
                }

        session.clipboard = clip;
        session.clipboardOrigin = min;
        ctx.sendMessage(Message.raw("§a[WE] Copied §e" + total + "§a blocks to clipboard."));
    }

    // ──────────────────────── paste ────────────────────────

    private void doPaste(CommandContext ctx, WandSession session, World world, PlayerRef player) {
        if (!session.hasClipboard()) {
            ctx.sendMessage(Message.raw("§c[WE] Clipboard is empty — use /we copy first."));
            return;
        }

        Vector3d pos = player.getTransform().getPosition();
        int ox = (int) Math.floor(pos.x);
        int oy = (int) Math.floor(pos.y);
        int oz = (int) Math.floor(pos.z);

        String[][][] clip = session.clipboard;
        int sx = clip.length, sy = clip[0].length, sz = clip[0][0].length;
        long total = (long) sx * sy * sz;

        if (total > WorldEditPlugin.MAX_BLOCKS) {
            ctx.sendMessage(Message.raw("§c[WE] Clipboard too large (" + total + " blocks). Max: " + WorldEditPlugin.MAX_BLOCKS));
            return;
        }

        // Save undo buffer
        String[][][] undo = new String[sx][sy][sz];
        for (int dx = 0; dx < sx; dx++)
            for (int dy = 0; dy < sy; dy++)
                for (int dz = 0; dz < sz; dz++) {
                    BlockType bt = world.getBlockType(ox + dx, oy + dy, oz + dz);
                    undo[dx][dy][dz] = (bt != null) ? bt.getId() : BlockType.EMPTY_KEY;
                }
        session.undoBuffer = undo;
        session.undoOrigin = new Vector3i(ox, oy, oz);

        // Paste
        int count = 0;
        for (int dx = 0; dx < sx; dx++)
            for (int dy = 0; dy < sy; dy++)
                for (int dz = 0; dz < sz; dz++) {
                    String blockId = clip[dx][dy][dz];
                    if (blockId != null) {
                        world.setBlock(ox + dx, oy + dy, oz + dz, blockId);
                        count++;
                    }
                }
        ctx.sendMessage(Message.raw("§a[WE] Pasted §e" + count + "§a blocks at (" + ox + ", " + oy + ", " + oz + ")"));
    }

    // ──────────────────────── undo ────────────────────────

    private void doUndo(CommandContext ctx, WandSession session, World world) {
        if (!session.hasUndo()) {
            ctx.sendMessage(Message.raw("§c[WE] Nothing to undo."));
            return;
        }

        String[][][] buf = session.undoBuffer;
        Vector3i origin = session.undoOrigin;
        int sx = buf.length, sy = buf[0].length, sz = buf[0][0].length;
        int count = 0;
        for (int dx = 0; dx < sx; dx++)
            for (int dy = 0; dy < sy; dy++)
                for (int dz = 0; dz < sz; dz++) {
                    String blockId = buf[dx][dy][dz];
                    if (blockId != null) {
                        world.setBlock(origin.x + dx, origin.y + dy, origin.z + dz, blockId);
                        count++;
                    }
                }

        session.undoBuffer = null;
        session.undoOrigin = null;
        ctx.sendMessage(Message.raw("§a[WE] Undid §e" + count + "§a blocks."));
    }

    // ──────────────────────── size ────────────────────────

    private void doSize(CommandContext ctx, WandSession session) {
        if (!session.hasSelection()) {
            ctx.sendMessage(Message.raw("§c[WE] No selection."));
            return;
        }
        ctx.sendMessage(Message.raw("§6[WE] Selection: §e"
            + session.getSizeX() + "§6 x §e" + session.getSizeY() + "§6 x §e" + session.getSizeZ()
            + " §6= §e" + session.getTotalBlocks() + " §6blocks"));
        Vector3i min = session.getMin(), max = session.getMax();
        ctx.sendMessage(Message.raw("§7  From §f(" + min.x + ", " + min.y + ", " + min.z + ")§7 to §f(" + max.x + ", " + max.y + ", " + max.z + ")"));
    }

    // ──────────────────────── help ────────────────────────

    private void sendHelp(CommandContext ctx) {
        ctx.sendMessage(Message.raw("§6[WE] Commands:"));
        ctx.sendMessage(Message.raw("§e/we wand §7— get WorldEdit wand"));
        ctx.sendMessage(Message.raw("§e/we pos1 §7/ §e/we pos2 §7— set positions at feet"));
        ctx.sendMessage(Message.raw("§e/we set <blockId> §7— fill selection"));
        ctx.sendMessage(Message.raw("§e/we walls <blockId> §7— fill walls of selection"));
        ctx.sendMessage(Message.raw("§e/we clear §7— fill selection with air"));
        ctx.sendMessage(Message.raw("§e/we copy §7— copy selection to clipboard"));
        ctx.sendMessage(Message.raw("§e/we paste §7— paste clipboard at feet"));
        ctx.sendMessage(Message.raw("§e/we undo §7— undo last operation"));
        ctx.sendMessage(Message.raw("§e/we size §7— show selection info"));
    }
}
