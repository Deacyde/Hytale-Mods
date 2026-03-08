package dev.deacyde.worldedit;

import com.hypixel.hytale.math.vector.Vector3i;

/**
 * Per-player WorldEdit session: stores pos1/pos2 selection, clipboard, and undo buffer.
 */
public class WandSession {

    public Vector3i pos1 = null;
    public Vector3i pos2 = null;

    // clipboard[dx][dy][dz] = blockId string; null entry means Air
    public String[][][] clipboard = null;
    // World-space origin of the clipboard (= pos1 corner when copy was taken)
    public Vector3i clipboardOrigin = null;

    // Undo buffer: blocks before the last fill/paste operation
    public String[][][] undoBuffer = null;
    // World-space origin of the undo buffer (= minX, minY, minZ of affected region)
    public Vector3i undoOrigin = null;

    public boolean hasSelection() {
        return pos1 != null && pos2 != null;
    }

    public boolean hasClipboard() {
        return clipboard != null && clipboardOrigin != null;
    }

    public boolean hasUndo() {
        return undoBuffer != null && undoOrigin != null;
    }

    /** Returns the min corner of pos1/pos2 selection. */
    public Vector3i getMin() {
        return new Vector3i(
            Math.min(pos1.x, pos2.x),
            Math.min(pos1.y, pos2.y),
            Math.min(pos1.z, pos2.z)
        );
    }

    /** Returns the max corner of pos1/pos2 selection. */
    public Vector3i getMax() {
        return new Vector3i(
            Math.max(pos1.x, pos2.x),
            Math.max(pos1.y, pos2.y),
            Math.max(pos1.z, pos2.z)
        );
    }

    public int getSizeX() { return Math.abs(pos1.x - pos2.x) + 1; }
    public int getSizeY() { return Math.abs(pos1.y - pos2.y) + 1; }
    public int getSizeZ() { return Math.abs(pos1.z - pos2.z) + 1; }
    public long getTotalBlocks() { return (long) getSizeX() * getSizeY() * getSizeZ(); }
}
