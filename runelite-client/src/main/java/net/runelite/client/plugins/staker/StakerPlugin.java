package net.runelite.client.plugins.staker;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.swing.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.HiscoreManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.LoopTimer;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.hiscore.HiscoreEndpoint;
import net.runelite.http.api.hiscore.HiscoreResult;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Slf4j
@PluginDescriptor(
        name = "Staker",
        description = "Find the best stakes and never get scammed.",
        enabledByDefault = true,
        loadWhenOutdated = true
)
public class StakerPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private StakerConfig config;

    @Inject
    private HiscoreManager hiscoreManager;

    private StakerPanel panel;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private StakerOverlay overlay;

    private NavigationButton navButton;
    public List<ChatMessage> messageList = new ArrayList<>();
    private LoopTimer scanTimer;
    public List<Player> playerList = new ArrayList<>();

    public HashMap<String, HiscoreResult> statsHashMap = new HashMap<>();

    public Player selectedPlayer;


    @Override
    protected void startUp() throws Exception {
        log.info("Staker started!");

        // Sidebar panel
        panel = new StakerPanel(this, config, client, hiscoreManager);
        panel.init();
        final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "attack.png");
        navButton = NavigationButton.builder()
                .tooltip("Staker")
                .icon(icon)
                .priority(1)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);

        // Overlays
        overlayManager.add(overlay);

        scanTimer = new LoopTimer(
                config.interval(),
                ChronoUnit.SECONDS,
                null,
                this,
                true);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Staker stopped!");

        // Toolbar
        clientToolbar.removeNavigation(navButton);

        // Overlays
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            client.addChatMessage(ChatMessageType.BROADCAST, "", "Staker Helper loaded", null);
        }

        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN || gameStateChanged.getGameState() == GameState.HOPPING) {
            log.info("Clearing...");
            messageList.removeAll(messageList);
            playerList.removeAll(playerList);
            SwingUtilities.invokeLater(panel::rebuild);
        }
    }

    List<Player> filteredPlayers() {
        try {
            if (client.getGameState() == GameState.LOGGED_IN && playerList.size() > 0) {
                List<Player> tempList = playerList;
                int gap = config.combat();
                int myCombatLevel = client.getLocalPlayer().getCombatLevel();
                tempList.removeIf(player -> (Math.abs(player.getCombatLevel() - myCombatLevel) > gap || player.getCombatLevel() > config.maxCombat()));
                return tempList;
            }
        } catch (Exception e) {
            log.error(e.toString());
            return null;
        }

        return null;
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event) {
        final Player local = client.getLocalPlayer();
        final Player player = event.getPlayer();

        if (player != local && !playerList.contains(player)) {
            playerList.add(player);
            playerList.sort(Comparator.comparingInt(Player::getCombatLevel));

//            playerList.sort(Comparator.nullsLast(Player::getCombatLevel));
        }
        SwingUtilities.invokeLater(panel::rebuild);
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        if (filteredPlayers() != null) {
            for (Player p : filteredPlayers()) {
                if (statsHashMap.get(p.getName()) == null) {
                    try {
                        HiscoreResult stats = hiscoreManager.lookupAsync(p.getName(), HiscoreEndpoint.NORMAL);
                        statsHashMap.put(p.getName(), stats);
                        SwingUtilities.invokeLater(this.panel::rebuild);
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
            }
        }
    }

    @Subscribe
    public void onPlayerDespawned(PlayerDespawned event) {
        playerList.remove(event.getPlayer());
        SwingUtilities.invokeLater(panel::rebuild);
    }


//    @Subscribe
//    public void onGameTick(GameTick gameTick) {
//    }

//    @Subscribe
//    public void onChatMessage(ChatMessage message) {
//        if (message.getType() != ChatMessageType.PUBLICCHAT) {
//            return;
//        }
//
//        log.info(message.toString());
//
//        messageList.add(message);
//        SwingUtilities.invokeLater(panel::rebuild);
//        log.info("Message added.");
//    }

    @Provides
    StakerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(StakerConfig.class);
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded) {
        if (!menuEntryAdded.getOption().equals("Challenge")
                || !config.highlight()) {
            return;
        }

        int npcIndex = menuEntryAdded.getIdentifier();
        Player player = client.getCachedPlayers()[npcIndex];
        if (player == null) {
            return;
        }

        if (player == selectedPlayer) {
            MenuEntry[] menuEntries = client.getMenuEntries();
            menuEntries[menuEntries.length - 1].setOption("Stake");
            menuEntries[menuEntries.length - 1].setTarget(ColorUtil.wrapWithColorTag(player.getName(), Color.cyan));
            menuEntries[menuEntries.length - 1].setForceLeftClick(true);
            client.setMenuEntries(menuEntries);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals("combat")) {
            messageList.removeAll(messageList);
            playerList.removeAll(playerList);
        }
    }
}
