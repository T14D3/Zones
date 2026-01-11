package de.t14d3.zones.visuals;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.zones.visuals.particles.ParticleRenderMode;

public class ParticleHandler {

    @FunctionalInterface
    public interface ParticlePointConsumer {
        /**
         * @return whether to continue emitting points
         */
        boolean accept(int x, int y, int z, boolean edgeOrCorner);
    }

    public static void emit(
            ParticleRenderMode mode,
            RBlockPos min,
            RBlockPos max,
            RBlockPos viewerBlock,
            int range,
            int step,
            ParticlePointConsumer consumer
    ) {
        if (mode == null || min == null || max == null || viewerBlock == null || consumer == null) return;

        int minX = Math.min(min.x(), max.x());
        int maxX = Math.max(min.x(), max.x());
        int minY = Math.min(min.y(), max.y());
        int maxY = Math.max(min.y(), max.y());
        int minZ = Math.min(min.z(), max.z());
        int maxZ = Math.max(min.z(), max.z());

        int x1 = minX;
        int y1 = minY;
        int z1 = minZ;
        int x2 = maxX + 1;
        int y2 = maxY + 1;
        int z2 = maxZ + 1;

        int cx1 = viewerBlock.x() - range;
        int cy1 = viewerBlock.y() - range;
        int cz1 = viewerBlock.z() - range;
        int cx2 = viewerBlock.x() + range;
        int cy2 = viewerBlock.y() + range;
        int cz2 = viewerBlock.z() + range;

        int s = Math.max(1, step);

        switch (mode) {
            case PERIMETER_AT_VIEWER_Y ->
                    emitPerimeterAtY(viewerBlock.y(), x1, x2, y1, y2, z1, z2, cx1, cx2, cz1, cz2, s, consumer);
            case WIRE_EDGES -> emitWireEdges(x1, x2, y1, y2, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, s, consumer);
            case FACES -> emitFaces(x1, x2, y1, y2, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, s, consumer);
        }
    }

    private static void emitPerimeterAtY(
            int y,
            int x1, int x2, int y1, int y2, int z1, int z2,
            int cx1, int cx2, int cz1, int cz2,
            int step,
            ParticlePointConsumer consumer
    ) {
        if (y < y1 || y > y2) return;

        CornerPredicate corners = (x, yy, z) -> isPerimeterCorner(x, z, x1, x2, z1, z2);
        if (!emitLineX(y, z1, x1, x2, cx1, cx2, cz1, cz2, step, corners, consumer)) return;
        if (!emitLineX(y, z2, x1, x2, cx1, cx2, cz1, cz2, step, corners, consumer)) return;
        if (!emitLineZ(x1, y, z1, z2, cx1, cx2, cz1, cz2, step, corners, consumer)) return;
        emitLineZ(x2, y, z1, z2, cx1, cx2, cz1, cz2, step, corners, consumer);
    }

    private static void emitWireEdges(
            int x1, int x2, int y1, int y2, int z1, int z2,
            int cx1, int cx2, int cy1, int cy2, int cz1, int cz2,
            int step,
            ParticlePointConsumer consumer
    ) {
        CornerPredicate corners = (x, y, z) -> isWireCorner(x, y, z, x1, x2, y1, y2, z1, z2);

        // Bottom rectangle
        if (!emitLineX(y1, z1, x1, x2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer)) return;
        if (!emitLineX(y1, z2, x1, x2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer)) return;
        if (!emitLineZ(x1, y1, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer)) return;
        if (!emitLineZ(x2, y1, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer)) return;

        // Top rectangle
        if (!emitLineX(y2, z1, x1, x2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer)) return;
        if (!emitLineX(y2, z2, x1, x2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer)) return;
        if (!emitLineZ(x1, y2, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer)) return;
        if (!emitLineZ(x2, y2, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer)) return;

        // Vertical edges
        if (!emitLineY(x1, z1, y1, y2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer)) return;
        if (!emitLineY(x1, z2, y1, y2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer)) return;
        if (!emitLineY(x2, z1, y1, y2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer)) return;
        emitLineY(x2, z2, y1, y2, cx1, cx2, cy1, cy2, cz1, cz2, step, corners, consumer);
    }

    private static void emitFaces(
            int x1, int x2, int y1, int y2, int z1, int z2,
            int cx1, int cx2, int cy1, int cy2, int cz1, int cz2,
            int step,
            ParticlePointConsumer consumer
    ) {
        // y faces
        if (!emitFaceY(y1, x1, x2, z1, z2, x1, x2, y1, y2, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, step, consumer))
            return;
        if (!emitFaceY(y2, x1, x2, z1, z2, x1, x2, y1, y2, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, step, consumer))
            return;

        // x faces
        if (!emitFaceX(x1, y1, y2, z1, z2, x1, x2, y1, y2, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, step, consumer))
            return;
        if (!emitFaceX(x2, y1, y2, z1, z2, x1, x2, y1, y2, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, step, consumer))
            return;

        // z faces
        if (!emitFaceZ(z1, x1, x2, y1, y2, x1, x2, y1, y2, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, step, consumer))
            return;
        emitFaceZ(z2, x1, x2, y1, y2, x1, x2, y1, y2, z1, z2, cx1, cx2, cy1, cy2, cz1, cz2, step, consumer);
    }

    private static boolean emitFaceY(
            int y,
            int xStart, int xEnd, int zStart, int zEnd,
            int x1, int x2, int y1, int y2, int z1, int z2,
            int cx1, int cx2, int cy1, int cy2, int cz1, int cz2,
            int step,
            ParticlePointConsumer consumer
    ) {
        if (y < cy1 || y > cy2) return true;

        int xs = Math.max(xStart, cx1);
        int xe = Math.min(xEnd, cx2);
        int zs = Math.max(zStart, cz1);
        int ze = Math.min(zEnd, cz2);
        if (xs > xe || zs > ze) return true;

        for (int x = xs; x <= xe; x += step) {
            for (int z = zs; z <= ze; z += step) {
                boolean edge = isOutlineEdge(x, y, z, x1, x2, y1, y2, z1, z2);
                if (!consumer.accept(x, y, z, edge)) return false;
            }
        }
        return true;
    }

    private static boolean emitFaceX(
            int x,
            int yStart, int yEnd, int zStart, int zEnd,
            int x1, int x2, int y1, int y2, int z1, int z2,
            int cx1, int cx2, int cy1, int cy2, int cz1, int cz2,
            int step,
            ParticlePointConsumer consumer
    ) {
        if (x < cx1 || x > cx2) return true;

        int ys = Math.max(yStart, cy1);
        int ye = Math.min(yEnd, cy2);
        int zs = Math.max(zStart, cz1);
        int ze = Math.min(zEnd, cz2);
        if (ys > ye || zs > ze) return true;

        for (int y = ys; y <= ye; y += step) {
            for (int z = zs; z <= ze; z += step) {
                boolean edge = isOutlineEdge(x, y, z, x1, x2, y1, y2, z1, z2);
                if (!consumer.accept(x, y, z, edge)) return false;
            }
        }
        return true;
    }

    private static boolean emitFaceZ(
            int z,
            int xStart, int xEnd, int yStart, int yEnd,
            int x1, int x2, int y1, int y2, int z1, int z2,
            int cx1, int cx2, int cy1, int cy2, int cz1, int cz2,
            int step,
            ParticlePointConsumer consumer
    ) {
        if (z < cz1 || z > cz2) return true;

        int xs = Math.max(xStart, cx1);
        int xe = Math.min(xEnd, cx2);
        int ys = Math.max(yStart, cy1);
        int ye = Math.min(yEnd, cy2);
        if (xs > xe || ys > ye) return true;

        for (int x = xs; x <= xe; x += step) {
            for (int y = ys; y <= ye; y += step) {
                boolean edge = isOutlineEdge(x, y, z, x1, x2, y1, y2, z1, z2);
                if (!consumer.accept(x, y, z, edge)) return false;
            }
        }
        return true;
    }

    @FunctionalInterface
    private interface CornerPredicate {
        boolean isCorner(int x, int y, int z);
    }

    private static boolean emitLineX(
            int y, int z,
            int xStart, int xEnd,
            int cx1, int cx2, int cy1, int cy2, int cz1, int cz2,
            int step,
            CornerPredicate corner,
            ParticlePointConsumer consumer
    ) {
        if (y < cy1 || y > cy2) return true;
        if (z < cz1 || z > cz2) return true;

        int xs = Math.max(xStart, cx1);
        int xe = Math.min(xEnd, cx2);
        if (xs > xe) return true;

        for (int x = xs; x <= xe; x += step) {
            if (!consumer.accept(x, y, z, corner.isCorner(x, y, z))) return false;
        }
        return true;
    }

    private static boolean emitLineX(
            int y, int z,
            int xStart, int xEnd,
            int cx1, int cx2, int cz1, int cz2,
            int step,
            CornerPredicate corner,
            ParticlePointConsumer consumer
    ) {
        if (z < cz1 || z > cz2) return true;

        int xs = Math.max(xStart, cx1);
        int xe = Math.min(xEnd, cx2);
        if (xs > xe) return true;

        for (int x = xs; x <= xe; x += step) {
            if (!consumer.accept(x, y, z, corner.isCorner(x, y, z))) return false;
        }
        return true;
    }

    private static boolean emitLineZ(
            int x, int y,
            int zStart, int zEnd,
            int cx1, int cx2, int cy1, int cy2, int cz1, int cz2,
            int step,
            CornerPredicate corner,
            ParticlePointConsumer consumer
    ) {
        if (x < cx1 || x > cx2) return true;
        if (y < cy1 || y > cy2) return true;

        int zs = Math.max(zStart, cz1);
        int ze = Math.min(zEnd, cz2);
        if (zs > ze) return true;

        for (int z = zs; z <= ze; z += step) {
            if (!consumer.accept(x, y, z, corner.isCorner(x, y, z))) return false;
        }
        return true;
    }

    private static boolean emitLineZ(
            int x, int y,
            int zStart, int zEnd,
            int cx1, int cx2, int cz1, int cz2,
            int step,
            CornerPredicate corner,
            ParticlePointConsumer consumer
    ) {
        if (x < cx1 || x > cx2) return true;

        int zs = Math.max(zStart, cz1);
        int ze = Math.min(zEnd, cz2);
        if (zs > ze) return true;

        for (int z = zs; z <= ze; z += step) {
            if (!consumer.accept(x, y, z, corner.isCorner(x, y, z))) return false;
        }
        return true;
    }

    private static boolean emitLineY(
            int x, int z,
            int yStart, int yEnd,
            int cx1, int cx2, int cy1, int cy2, int cz1, int cz2,
            int step,
            CornerPredicate corner,
            ParticlePointConsumer consumer
    ) {
        if (x < cx1 || x > cx2) return true;
        if (z < cz1 || z > cz2) return true;

        int ys = Math.max(yStart, cy1);
        int ye = Math.min(yEnd, cy2);
        if (ys > ye) return true;

        for (int y = ys; y <= ye; y += step) {
            if (!consumer.accept(x, y, z, corner.isCorner(x, y, z))) return false;
        }
        return true;
    }

    private static boolean isOutlineEdge(int x, int y, int z, int x1, int x2, int y1, int y2, int z1, int z2) {
        boolean xEdge = x == x1 || x == x2;
        boolean yEdge = y == y1 || y == y2;
        boolean zEdge = z == z1 || z == z2;
        return (xEdge && yEdge) || (xEdge && zEdge) || (yEdge && zEdge);
    }

    private static boolean isPerimeterCorner(int x, int z, int x1, int x2, int z1, int z2) {
        boolean xEdge = x == x1 || x == x2;
        boolean zEdge = z == z1 || z == z2;
        return xEdge && zEdge;
    }

    private static boolean isWireCorner(int x, int y, int z, int x1, int x2, int y1, int y2, int z1, int z2) {
        boolean xEdge = x == x1 || x == x2;
        boolean yEdge = y == y1 || y == y2;
        boolean zEdge = z == z1 || z == z2;
        return xEdge && yEdge && zEdge;
    }

    public static long distanceSquared(int ax, int ay, int az, int bx, int by, int bz) {
        long dx = (long) bx - ax;
        long dy = (long) by - ay;
        long dz = (long) bz - az;
        return dx * dx + dy * dy + dz * dz;
    }

    public static long packBlockPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFF);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 38);
    }

    public static int unpackY(long packed) {
        return (int) (packed << 52 >> 52);
    }

    public static int unpackZ(long packed) {
        return (int) (packed << 26 >> 38);
    }
}
