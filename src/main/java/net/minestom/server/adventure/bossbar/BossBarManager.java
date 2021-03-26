package net.minestom.server.adventure.bossbar;

import com.google.common.collect.MapMaker;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.network.packet.server.play.BossBarPacket;
import net.minestom.server.utils.PacketUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages all boss bars known to this Minestom instance. Although this class can be used
 * to show boss bars to players, it is preferable to use the boss bar methods in the
 * {@link Audience} class instead.
 *
 * <p>This implementation is heavily based on
 * <a href="https://github.com/VelocityPowered/Velocity">Velocity</a>'s boss bar
 * management system.</p>
 *
 * @see Audience#showBossBar(BossBar)
 * @see Audience#hideBossBar(BossBar)
 */
public class BossBarManager {
    private final BossBarListener listener;
    private final Map<UUID, Set<BossBarHolder>> playerBars;
    final Map<BossBar, BossBarHolder> bars;

    /**
     * Creates a new boss bar manager.
     */
    public BossBarManager() {
        this.listener = new BossBarListener(this);
        this.playerBars = new ConcurrentHashMap<>();
        this.bars = new ConcurrentHashMap<>();
    }

    /**
     * Adds the specified player to the boss bar's viewers and spawns the boss bar, registering the
     * boss bar if needed.
     *
     * @param player the intended viewer
     * @param bar the boss bar to show
     */
    public void addBossBar(@NotNull Player player, @NotNull BossBar bar) {
        BossBarHolder holder = this.getOrCreateHandler(bar);

        if (holder.addViewer(player)) {
            player.getPlayerConnection().sendPacket(holder.createAddPacket());
            this.playerBars.computeIfAbsent(player.getUuid(), uuid -> new HashSet<>()).add(holder);
        }
    }

    /**
     * Removes the specified player from the boss bar's viewers and despawns the boss bar.
     *
     * @param player the intended viewer
     * @param bar the boss bar to hide
     */
    public void removeBossBar(@NotNull Player player, @NotNull BossBar bar) {
        BossBarHolder holder = this.getOrCreateHandler(bar);

        if (holder.removeViewer(player)) {
            player.getPlayerConnection().sendPacket(holder.createRemovePacket());
            this.removePlayer(player, holder);
        }
    }

    /**
     * Adds the specified players to the boss bar's viewers and spawns the boss bar, registering the
     * boss bar if needed.
     *
     * @param players the players
     * @param bar the boss bar
     */
    public void addBossBar(@NotNull Collection<Player> players, @NotNull BossBar bar) {
        BossBarHolder holder = this.getOrCreateHandler(bar);
        Collection<Player> addedPlayers = new ArrayList<>();

        for (Player player : players) {
            if (holder.addViewer(player)) {
                addedPlayers.add(player);
                this.playerBars.computeIfAbsent(player.getUuid(), uuid -> new HashSet<>()).add(holder);
            }
        }

        if (!addedPlayers.isEmpty()) {
            PacketUtils.sendGroupedPacket(addedPlayers, holder.createAddPacket());
        }
    }

    /**
     * Removes the specified players from the boss bar's viewers and despawns the boss bar.
     *
     * @param players the intended viewers
     * @param bar the boss bar to hide
     */
    public void removeBossBar(@NotNull Collection<Player> players, @NotNull BossBar bar) {
        BossBarHolder holder = this.getOrCreateHandler(bar);
        Collection<Player> removedPlayers = new ArrayList<>();

        for (Player player : players) {
            if (holder.removeViewer(player)) {
                removedPlayers.add(player);
                this.removePlayer(player, holder);
            }
        }

        if (!removedPlayers.isEmpty()) {
            PacketUtils.sendGroupedPacket(removedPlayers, holder.createRemovePacket());
        }
    }

    /**
     * Completely destroys a boss bar, removing it from all players.
     *
     * @param bossBar the boss bar
     */
    public void destroyBossBar(@NotNull BossBar bossBar) {
        BossBarHolder holder = this.bars.remove(bossBar);

        if (holder != null) {
            PacketUtils.sendGroupedPacket(holder.players, holder.createRemovePacket());

            for (Player player : holder.players) {
                this.removePlayer(player, holder);
            }
        }
    }

    /**
     * Removes a player from all of their boss bars. Note that this method does not
     * send any removal packets to the player. It is meant to be used when a player is
     * disconnecting from the server.
     *
     * @param player the player
     */
    public void removeAllBossBars(@NotNull Player player) {
        Set<BossBarHolder> holders = this.playerBars.remove(player.getUuid());

        if (holders != null) {
            for (BossBarHolder holder : holders) {
                holder.removeViewer(player);
            }
        }
    }

    /**
     * Gets or creates a handler for this bar.
     *
     * @param bar the bar
     *
     * @return the handler
     */
    private @NotNull BossBarHolder getOrCreateHandler(@NotNull BossBar bar) {
        return this.bars.computeIfAbsent(bar, key -> {
            BossBarHolder holder = new BossBarHolder(key);
            bar.addListener(this.listener);
            return holder;
        });
    }

    private void removePlayer(Player player, BossBarHolder holder) {
        Set<BossBarHolder> holders = this.playerBars.get(player.getUuid());

        if (holders != null) {
            holders.remove(holder);

            if (holders.isEmpty()) {
                this.playerBars.remove(player.getUuid());
            }
        }
    }
}
