package net.runelite.client.plugins.staker;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;

public class StakerOverlay extends Overlay {
    private final Client client;
    private final StakerConfig config;
    private final StakerPlugin plugin;

    @Inject
    private StakerOverlay(Client client, StakerPlugin plugin, StakerConfig config) {
        super(plugin);
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    private void renderTile(final Graphics2D graphics, final LocalPoint dest, final Color color) {
        if (dest == null) {
            return;
        }

        final Polygon poly = Perspective.getCanvasTilePoly(client, dest);

        if (poly == null) {
            return;
        }

        OverlayUtil.renderPolygon(graphics, poly, color);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (config.highlight()) {
            if (plugin.filteredPlayers() != null) {
                for (Player player : plugin.filteredPlayers()) {
                    Color combatColor = Color.yellow;
                    if (player.getCombatLevel() > client.getLocalPlayer().getCombatLevel()) {
                        combatColor = Color.red;
                    } else if (player.getCombatLevel() < client.getLocalPlayer().getCombatLevel()) {
                        combatColor = Color.green;
                    }
                    renderTile(graphics, LocalPoint.fromWorld(client, player.getWorldLocation()), combatColor);
                }
            }

            if (plugin.selectedPlayer != null) {
                // Tile
                renderTile(graphics, LocalPoint.fromWorld(client, plugin.selectedPlayer.getWorldLocation()), Color.CYAN);

                // Hull
                Polygon polygon = plugin.selectedPlayer.getCanvasTilePoly();
                if (polygon != null) {
                    OverlayUtil.renderActorOverlay(graphics, plugin.selectedPlayer, plugin.selectedPlayer.getName(), Color.CYAN);
                    OverlayUtil.renderPolygon(graphics, polygon, Color.CYAN);
                }
            }
        }

        return null;
    }
}
