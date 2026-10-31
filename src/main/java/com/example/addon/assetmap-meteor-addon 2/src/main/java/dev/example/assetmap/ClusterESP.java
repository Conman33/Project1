package dev.example.assetmap;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Meteor module (Mojang mappings, MC 26.x): outlines dense groups of containers.
 * Main thread copies packed positions -> worker thread clusters -> volatile immutable result -> Render3D draws it.
 */
public class ClusterESP extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Integer> linkRadius = sg.add(new IntSetting.Builder()
        .name("link-radius").description("Max distance between two containers to count as the same group.")
        .defaultValue(8).min(2).sliderMax(24).build());

    private final Setting<Integer> minCount = sg.add(new IntSetting.Builder()
        .name("min-containers").description("Minimum containers in a group to highlight it.")
        .defaultValue(16).min(2).sliderMax(100).build());

    private final Setting<Boolean> drawMembers = sg.add(new BoolSetting.Builder()
        .name("outline-each-container").description("Also outline every container in the group.")
        .defaultValue(true).build());

    private final Setting<Integer> scanTicks = sg.add(new IntSetting.Builder()
        .name("scan-interval-ticks").description("How often to rescan block entities.")
        .defaultValue(20).min(5).sliderMax(100).build());

    private final Setting<SettingColor> color = sg.add(new ColorSetting.Builder()
        .name("color").description("Outline color.")
        .defaultValue(new SettingColor(255, 140, 40, 150)).build());

    private final Setting<ShapeMode> shapeMode = sg.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode").defaultValue(ShapeMode.Lines).build());

    private volatile List<ClusterAnalyzer.Cluster> clusters = List.of();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private ExecutorService worker;
    private int timer;

    public ClusterESP() {
        super(Categories.Render, "cluster-esp", "Outlines dense groups of containers and machinery.");
    }

    @Override
    public void onActivate() {
        worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "cluster-esp-worker");
            t.setDaemon(true);
            return t;
        });
        timer = 0;
        busy.set(false);
    }

    @Override
    public void onDeactivate() {
        if (worker != null) { worker.shutdownNow(); worker = null; }
        clusters = List.of();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null || worker == null) return;
        if (++timer < scanTicks.get()) return;
        timer = 0;
        if (!busy.compareAndSet(false, true)) return;

        long[] snap = new long[64];
        int n = 0;
        for (BlockEntity be : Utils.blockEntities()) {
            if (!(be instanceof BaseContainerBlockEntity)) continue;
            if (n == snap.length) snap = Arrays.copyOf(snap, n * 2);
            snap[n++] = be.getBlockPos().asLong();
        }
        final long[] pts = Arrays.copyOf(snap, n);
        final int r = linkRadius.get(), min = minCount.get();

        worker.submit(() -> {
            try { clusters = ClusterAnalyzer.analyze(pts, r, min); }
            catch (Throwable t) { t.printStackTrace(); }
            finally { busy.set(false); }
        });
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        SettingColor c = color.get();
        SettingColor fill = new SettingColor(c.r, c.g, c.b, Math.max(10, c.a / 4));
        for (ClusterAnalyzer.Cluster cl : clusters) {
            event.renderer.box(cl.minX() - 0.5, cl.minY() - 0.5, cl.minZ() - 0.5,
                               cl.maxX() + 1.5, cl.maxY() + 1.5, cl.maxZ() + 1.5, fill, c, shapeMode.get(), 0);
            if (drawMembers.get()) {
                for (long p : cl.members()) {
                    int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                    event.renderer.box(x, y, z, x + 1, y + 1, z + 1, fill, c, shapeMode.get(), 0);
                }
            }
        }
    }
}
