/*
 * Copyright (c) 2018 Abex
 * Copyright (c) 2018, Psikoi <https://github.com/psikoi>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.staker;

import com.google.inject.Inject;

import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.account.SessionManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.HiscoreManager;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

@Slf4j
@Singleton
public class StakerPanel extends PluginPanel {
    private static final ImageIcon ICON;

    static {
        ICON = new ImageIcon(ImageUtil.getResourceStreamFromClass(StakerPanel.class, "attack.png"));
    }

    private HiscoreManager hiscoreManager;

    private Client client;

    private final StakerPlugin plugin;

    private final StakerConfig config;

    private JPanel header;


    StakerPanel(final StakerPlugin plugin, final StakerConfig config, final Client client, final HiscoreManager hiscoreManager) {
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        this.hiscoreManager = hiscoreManager;
    }

    private List<StakerBox> msgs;
    private JPanel listPanel;

    private JLabel playerCount = new JLabel("0 players", SwingConstants.LEFT);

    void init() {
        setAutoscrolls(false);
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setAlignmentX(0.0f);

        JLabel label = new JLabel();
        label.setText("Staker Helper");
        label.setFont(getFont().deriveFont(32f));
        label.setVisible(true);
        label.setAlignmentX(SwingConstants.LEFT);
        header.add(label);
        header.add(playerCount);

        add(header, BorderLayout.NORTH);


        this.listPanel = new JPanel();
        listPanel.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
        this.listPanel.setVisible(true);
        this.listPanel.add(new JLabel("Waiting..."));
        add(listPanel, BorderLayout.CENTER);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> {
            plugin.playerList.removeAll(plugin.playerList);
            plugin.messageList.removeAll(plugin.messageList);
            rebuild();
        });
        add(refresh, BorderLayout.SOUTH);
    }

    void rebuild() {
        // Rebuild label
        if (plugin.filteredPlayers() != null) {
            playerCount.setText("Players: " + plugin.filteredPlayers().size());
        } else {
            playerCount.setText("No players");
        }

        // Rebuild player list
        listPanel.removeAll();
        try {
            if (plugin.filteredPlayers() != null) {
                for (Player player : plugin.filteredPlayers()) {
                    listPanel.add(new StakerBox(plugin, config, player, client, hiscoreManager));
                }
            } else {
                listPanel.add(new JLabel("Waiting..."));
            }
        } catch (Exception e) {
            listPanel.add(new JLabel("Error. " + e.toString()));
        }
        revalidate();
        repaint();
    }

    private static String htmlLabel(String key, String value) {
        return "<html><body style = 'color:#a5a5a5'>" + key + "<span style = 'color:white'>" + value + "</span></body></html>";
    }

}
