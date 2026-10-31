package dev.example.assetmap;

import net.minecraft.core.BlockPos;

import java.util.*;

/** Spatial-hash + union-find connected components. O(n) average. Pure function, no game objects. */
public final class ClusterAnalyzer {
    private ClusterAnalyzer() {}

    public record Cluster(int count, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, long[] members) {}

    /**
     * @param radius   max distance between two blocks to be linked (single-linkage)
     * @param minCount smallest component size to report (16 == "more than 15")
     */
    public static List<Cluster> analyze(long[] pts, int radius, int minCount) {
        int n = pts.length;
        if (n < minCount) return List.of();

        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        Map<Long, List<Integer>> grid = new HashMap<>(n * 2);
        for (int i = 0; i < n; i++) {
            grid.computeIfAbsent(cell(pts[i], radius), k -> new ArrayList<>(4)).add(i);
        }

        long r2 = (long) radius * radius;
        for (int i = 0; i < n; i++) {
            int x = BlockPos.getX(pts[i]), y = BlockPos.getY(pts[i]), z = BlockPos.getZ(pts[i]);
            int cx = Math.floorDiv(x, radius), cy = Math.floorDiv(y, radius), cz = Math.floorDiv(z, radius);
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        List<Integer> bucket = grid.get(BlockPos.asLong(cx + dx, cy + dy, cz + dz));
                        if (bucket == null) continue;
                        for (int j : bucket) {
                            if (j <= i) continue;
                            long ox = BlockPos.getX(pts[j]) - x;
                            long oy = BlockPos.getY(pts[j]) - y;
                            long oz = BlockPos.getZ(pts[j]) - z;
                            if (ox * ox + oy * oy + oz * oz <= r2) union(parent, i, j);
                        }
                    }
        }

        Map<Integer, List<Integer>> comps = new HashMap<>();
        for (int i = 0; i < n; i++) comps.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(i);

        List<Cluster> out = new ArrayList<>();
        for (List<Integer> c : comps.values()) {
            if (c.size() < minCount) continue;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            long[] members = new long[c.size()];
            int k = 0;
            for (int idx : c) {
                long p = pts[idx];
                members[k++] = p;
                int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
            }
            out.add(new Cluster(c.size(), minX, minY, minZ, maxX, maxY, maxZ, members));
        }
        return List.copyOf(out);
    }

    private static long cell(long p, int r) {
        return BlockPos.asLong(Math.floorDiv(BlockPos.getX(p), r),
                               Math.floorDiv(BlockPos.getY(p), r),
                               Math.floorDiv(BlockPos.getZ(p), r));
    }

    private static int find(int[] p, int x) {
        while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; }
        return x;
    }

    private static void union(int[] p, int a, int b) {
        int ra = find(p, a), rb = find(p, b);
        if (ra != rb) p[ra] = rb;
    }
}
