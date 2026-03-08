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
 *  hollow            — remove interior blocks, keep outer shell
 *  copy              — copy selection to clipboard (origin = pos1 corner)
 *  cut               — copy + clear original; clipboard ready to paste/rotate
 *  paste             — paste clipboard at player's feet
 *  rotate <deg>      — rotate clipboard 90/180/270° around Y axis before paste
 *  stack <n> <dir>   — repeat selection N times in direction (north/south/east/west/up/down)
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
            case "clear"  -> doFill(ctx, session, world, BlockType.EMPTY_KEY, false);
            case "hollow" -> doHollow(ctx, session, world);
            case "copy"   -> doCopy(ctx, session, world, player);
            case "cut"    -> doCut(ctx, session, world, player);
            case "paste"  -> doPaste(ctx, session, world, player);
            case "rotate" -> {
                if (args.length < 2) { ctx.sendMessage(Message.raw("§c[WE] Usage: /we rotate <90|180|270>")); return; }
                doRotate(ctx, session, args[1]);
            }
            case "stack" -> {
                if (args.length < 3) { ctx.sendMessage(Message.raw("§c[WE] Usage: /we stack <n> <north|south|east|west|up|down>")); return; }
                doStack(ctx, session, world, args[1], args[2]);
            }
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

    // ──────────────────────── hollow ────────────────────────

    private void doHollow(CommandContext ctx, WandSession session, World world) {
        if (!session.hasSelection()) {
            ctx.sendMessage(Message.raw("§c[WE] No selection."));
            return;
        }
        Vector3i min = session.getMin();
        Vector3i max = session.getMax();
        int sx = session.getSizeX(), sy = session.getSizeY(), sz = session.getSizeZ();
        long total = session.getTotalBlocks();

        if (total > WorldEditPlugin.MAX_BLOCKS) {
            ctx.sendMessage(Message.raw("§c[WE] Selection too large (" + total + " blocks). Max: " + WorldEditPlugin.MAX_BLOCKS));
            return;
        }

        // Save undo
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

        // Clear only interior blocks (not on any face)
        int count = 0;
        for (int x = min.x; x <= max.x; x++) {
            for (int y = min.y; y <= max.y; y++) {
                for (int z = min.z; z <= max.z; z++) {
                    boolean onFace = x == min.x || x == max.x
                                  || y == min.y || y == max.y
                                  || z == min.z || z == max.z;
                    if (!onFace) {
                        world.setBlock(x, y, z, BlockType.EMPTY_KEY);
                        count++;
                    }
                }
            }
        }
        ctx.sendMessage(Message.raw("§a[WE] Hollowed — cleared §e" + count + "§a interior blocks."));
    }

    // ──────────────────────── cut ────────────────────────

    private void doCut(CommandContext ctx, WandSession session, World world, PlayerRef player) {
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

        // Copy to clipboard
        String[][][] clip = new String[sx][sy][sz];
        for (int x = min.x; x <= max.x; x++)
            for (int y = min.y; y <= max.y; y++)
                for (int z = min.z; z <= max.z; z++) {
                    BlockType bt = world.getBlockType(x, y, z);
                    clip[x - min.x][y - min.y][z - min.z] = (bt != null) ? bt.getId() : BlockType.EMPTY_KEY;
                }
        session.clipboard = clip;
        session.clipboardOrigin = min;

        // Save undo (the cut region — so undo restores it)
        if (total <= WorldEditPlugin.UNDO_SKIP_THRESHOLD) {
            session.undoBuffer = clip; // same data — undo re-places what was cut
            session.undoOrigin = min;
        } else {
            session.undoBuffer = null;
            session.undoOrigin = null;
        }

        // Clear original blocks
        for (int x = min.x; x <= max.x; x++)
            for (int y = min.y; y <= max.y; y++)
                for (int z = min.z; z <= max.z; z++)
                    world.setBlock(x, y, z, BlockType.EMPTY_KEY);

        ctx.sendMessage(Message.raw("§a[WE] Cut §e" + total + "§a blocks to clipboard. Use §f/we paste§a to place, §f/we rotate§a to reorient first."));
    }

    // ──────────────────────── rotate ────────────────────────

    private void doRotate(CommandContext ctx, WandSession session, String degreesArg) {
        if (!session.hasClipboard()) {
            ctx.sendMessage(Message.raw("§c[WE] Clipboard is empty — use /we copy or /we cut first."));
            return;
        }
        int degrees;
        try {
            degrees = Integer.parseInt(degreesArg.replace("°", ""));
        } catch (NumberFormatException e) {
            ctx.sendMessage(Message.raw("§c[WE] Usage: /we rotate <90|180|270>"));
            return;
        }
        int steps = ((degrees / 90) % 4 + 4) % 4;
        if (steps == 0) {
            ctx.sendMessage(Message.raw("§7[WE] Rotated 0° — clipboard unchanged."));
            return;
        }

        String[][][] clip = session.clipboard;
        for (int i = 0; i < steps; i++) {
            clip = rotate90(clip);
        }
        session.clipboard = clip;
        ctx.sendMessage(Message.raw("§a[WE] Clipboard rotated §e" + (steps * 90) + "°§a. Use §f/we paste§a to place."));
    }

    /** Rotate a 3D block array 90° clockwise around the Y axis. X→Z, Z→-X. */
    private String[][][] rotate90(String[][][] src) {
        int sx = src.length, sy = src[0].length, sz = src[0][0].length;
        // After 90° CW (top-down): new[z][y][sx-1-x] = old[x][y][z]  →  new dims: sz x sy x sx
        String[][][] dst = new String[sz][sy][sx];
        for (int x = 0; x < sx; x++)
            for (int y = 0; y < sy; y++)
                for (int z = 0; z < sz; z++)
                    dst[z][y][sx - 1 - x] = src[x][y][z];
        return dst;
    }

    // ──────────────────────── stack ────────────────────────

    private void doStack(CommandContext ctx, WandSession session, World world, String countArg, String dirArg) {
        if (!session.hasSelection()) {
            ctx.sendMessage(Message.raw("§c[WE] No selection."));
            return;
        }
        int n;
        try {
            n = Integer.parseInt(countArg);
            if (n < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ctx.sendMessage(Message.raw("§c[WE] Usage: /we stack <n> <north|south|east|west|up|down>"));
            return;
        }

        int[] step;
        switch (dirArg.toLowerCase()) {
            case "north" -> step = new int[]{0, 0, -session.getSizeZ()};
            case "south" -> step = new int[]{0, 0,  session.getSizeZ()};
            case "east"  -> step = new int[]{ session.getSizeX(), 0, 0};
            case "west"  -> step = new int[]{-session.getSizeX(), 0, 0};
            case "up"    -> step = new int[]{0,  session.getSizeY(), 0};
            case "down"  -> step = new int[]{0, -session.getSizeY(), 0};
            default -> {
                ctx.sendMessage(Message.raw("§c[WE] Direction must be: north south east west up down"));
                return;
            }
        }

        long totalOp = session.getTotalBlocks() * n;
        if (totalOp > WorldEditPlugin.MAX_BLOCKS) {
            ctx.sendMessage(Message.raw("§c[WE] Stack too large (" + totalOp + " blocks). Max: " + WorldEditPlugin.MAX_BLOCKS));
            return;
        }

        Vector3i min = session.getMin();
        Vector3i max = session.getMax();

        // Read source once
        int sx = session.getSizeX(), sy = session.getSizeY(), sz = session.getSizeZ();
        String[][][] src = new String[sx][sy][sz];
        for (int x = min.x; x <= max.x; x++)
            for (int y = min.y; y <= max.y; y++)
                for (int z = min.z; z <= max.z; z++) {
                    BlockType bt = world.getBlockType(x, y, z);
                    src[x - min.x][y - min.y][z - min.z] = (bt != null) ? bt.getId() : BlockType.EMPTY_KEY;
                }

        long placed = 0;
        for (int rep = 1; rep <= n; rep++) {
            int ox = min.x + step[0] * rep;
            int oy = min.y + step[1] * rep;
            int oz = min.z + step[2] * rep;
            for (int dx = 0; dx < sx; dx++)
                for (int dy = 0; dy < sy; dy++)
                    for (int dz = 0; dz < sz; dz++) {
                        String blockId = src[dx][dy][dz];
                        if (blockId != null) {
                            world.setBlock(ox + dx, oy + dy, oz + dz, blockId);
                            placed++;
                        }
                    }
        }
        ctx.sendMessage(Message.raw("§a[WE] Stacked §e" + n + "x §a" + dirArg + " — §e" + placed + "§a blocks placed."));
    }



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
        ctx.sendMessage(Message.raw("§e/we hollow §7— remove interior, keep outer shell"));
        ctx.sendMessage(Message.raw("§e/we copy §7— copy selection to clipboard"));
        ctx.sendMessage(Message.raw("§e/we cut §7— copy + clear original (ready to paste)"));
        ctx.sendMessage(Message.raw("§e/we paste §7— paste clipboard at feet"));
        ctx.sendMessage(Message.raw("§e/we rotate <90|180|270> §7— rotate clipboard before paste"));
        ctx.sendMessage(Message.raw("§e/we stack <n> <north|south|east|west|up|down> §7— repeat selection N times"));
        ctx.sendMessage(Message.raw("§e/we undo §7— undo last operation"));
        ctx.sendMessage(Message.raw("§e/we size §7— show selection info"));
    }
}
