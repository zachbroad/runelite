package net.runelite.client.plugins.staker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.client.game.HiscoreManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.http.api.hiscore.HiscoreClient;
import net.runelite.http.api.hiscore.HiscoreEndpoint;
import net.runelite.http.api.hiscore.HiscoreResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@Slf4j
public class StakerBox extends JPanel {

    private final JLabel name = new JLabel();
    private final JLabel text = new JLabel();
    private final JPanel comparisonPanel = new JPanel();

    private Client client;
    private HiscoreManager hiscoreManager;
    private HiscoreResult stats;
    private StakerPlugin plugin;
    private StakerConfig config;


    private Player player;

    StakerBox(StakerPlugin plugin, StakerConfig config, final Player player, final Client client, final HiscoreManager hiscoreManager) {
        this.plugin = plugin;
        this.player = player;
        this.client = client;
        this.config = config;
        this.hiscoreManager = hiscoreManager;

        setLayout(new BorderLayout(0, 1));
        setBorder(new EmptyBorder(10, 0, 0, 0));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        Color combatColor = Color.yellow;
        if (player.getCombatLevel() > client.getLocalPlayer().getCombatLevel()) {
            combatColor = Color.red;
        } else if (player.getCombatLevel() < client.getLocalPlayer().getCombatLevel()) {
            combatColor = Color.green;
        }

        name.setText(String.format("[%d] %s", player.getCombatLevel(), player.getName()));
        name.setForeground(combatColor);
        name.setFont(FontManager.getRunescapeBoldFont());
        add(name, BorderLayout.NORTH);

        if (plugin.statsHashMap.get(player.getName()) != null) {
            if(deltaTotal() < 0) {
               setVisible(false);
            }
        }

        if (plugin.statsHashMap.get(player.getName()) != null) {

            HiscoreResult st = plugin.statsHashMap.get(player.getName());

            final int opAttack = st.getAttack().getLevel();
            final int opStrength = st.getStrength().getLevel();
            final int opDefence = st.getDefence().getLevel();
            final int opHitpoints = st.getHitpoints().getLevel();

            final JLabel comp = new JLabel(String.format("Attack: %d", deltaAttack()));
            comp.setFont(FontManager.getRunescapeFont());
            comparisonPanel.add(comp);
            JLabel comp1 = new JLabel(String.format("Strength: %d", deltaStrength()));
            comparisonPanel.add(comp1);
            JLabel comp2 = new JLabel(String.format("Defence: %d", deltaDefence()));
            comp2.setFont(FontManager.getRunescapeFont());
            comparisonPanel.add(comp2);
            JLabel comp3 = new JLabel(String.format("Hitpoints: %d", deltaHitpoints()));
            comp3.setFont(FontManager.getRunescapeFont());
            comparisonPanel.add(comp3);

            JLabel total = new JLabel("Total: " + deltaTotal());
            total.setFont(FontManager.getRunescapeBoldFont());
            if (deltaTotal() > 0) {
                total.setForeground(Color.yellow);
            } else {
                total.setForeground(Color.red);
            }
            if (deltaTotal() > config.goodStake()) {
                total.setForeground(Color.green);
            }
            comparisonPanel.add(total);

            JLabel odds = new JLabel("Odds: " + getOdds());
            odds.setFont(FontManager.getRunescapeBoldFont());
            if (getOdds() > 0) {
                odds.setForeground(Color.yellow);
            } else {
                odds.setForeground(Color.red);
            }
            if (getOdds() > config.goodStake()) {
                odds.setForeground(Color.green);
            }
            comparisonPanel.add(odds);
        } else {
            comparisonPanel.add(new JLabel("Stats not loaded"));
        }
        comparisonPanel.setLayout(new BoxLayout(comparisonPanel, BoxLayout.Y_AXIS));
        comparisonPanel.setLayout(new GridLayout(3, 2));
        comparisonPanel.setVisible(true);
        add(comparisonPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);

        JButton highlight = new JButton(plugin.selectedPlayer == player ? "Un-Highlight" : "Highlight");
        highlight.addActionListener(e -> {
            if (client.getHintArrowPlayer() == player) {
                highlight.setText("Highlight");
                client.clearHintArrow();
                plugin.selectedPlayer = null;

                revalidate();
                repaint();
            } else {
                highlight.setText("Un-Highlight");
                plugin.selectedPlayer = player;
                client.setHintArrow(player);
                client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", String.format("Highlighted [%d] %s", player.getCombatLevel(), player.getName()), "");
                revalidate();
                repaint();
            }
        });
        buttonPanel.add(highlight);

        JButton lookup = new JButton("Lookup");
        lookup.addActionListener(e ->
        {
            lookup.setText("Loading...");
            HiscoreClient hiscoreClient = new HiscoreClient();
            try {
                stats = hiscoreManager.lookupAsync(player.getName(), HiscoreEndpoint.NORMAL);
                plugin.statsHashMap.put(player.getName(), stats);
                SwingUtilities.invokeLater(this::rebuild);
            } catch (Exception err) {
                err.printStackTrace();
                lookup.setText("Error");
            }

            lookup.setText("Success");
        });
        buttonPanel.add(lookup);

        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);

    }

    int deltaAttack() {
        if (plugin.statsHashMap.get(player.getName()) != null) {
            return client.getRealSkillLevel(Skill.ATTACK) - plugin.statsHashMap.get(player.getName()).getAttack().getLevel();
        } else {
            return 0;
        }
    }

    int deltaStrength() {
        if (plugin.statsHashMap.get(player.getName()) != null) {
            return client.getRealSkillLevel(Skill.STRENGTH) - plugin.statsHashMap.get(player.getName()).getStrength().getLevel();
        } else {
            return 0;
        }
    }

    int deltaDefence() {
        if (plugin.statsHashMap.get(player.getName()) != null) {
            return client.getRealSkillLevel(Skill.DEFENCE) - plugin.statsHashMap.get(player.getName()).getDefence().getLevel();
        } else {
            return 0;
        }
    }

    int deltaHitpoints() {
        if (plugin.statsHashMap.get(player.getName()) != null) {
            return client.getRealSkillLevel(Skill.HITPOINTS) - plugin.statsHashMap.get(player.getName()).getHitpoints().getLevel();
        } else {
            return 0;
        }
    }

    int deltaTotal() {
        return deltaAttack() + deltaStrength() + deltaDefence() + deltaHitpoints();
    }

    double getOdds() {
        if (plugin.statsHashMap.get(player.getName()) != null) {
            final int attack = client.getRealSkillLevel(Skill.ATTACK);
            final int strength = client.getRealSkillLevel(Skill.STRENGTH);
            final int defense = client.getRealSkillLevel(Skill.DEFENCE);
            final int hp = client.getRealSkillLevel(Skill.HITPOINTS);

            int deltaAttack = deltaAttack();
            int deltaStrength = deltaStrength();
            int deltaDefense = deltaDefence();
            int deltaHitpoints = deltaHitpoints();

            double odds = (1.05 * (deltaAttack + deltaStrength + deltaDefense)) + (1.1 * (deltaHitpoints));
            int total = deltaAttack + deltaStrength + deltaDefense + deltaHitpoints;

            return odds;
        } else {
            return 1.0f;
        }
    }

    void rebuild() {
        comparisonPanel.removeAll();
        if (plugin.statsHashMap.get(player.getName()) != null) {
            final int opAttack = stats.getAttack().getLevel();
            final int opStrength = stats.getStrength().getLevel();
            final int opDefence = stats.getDefence().getLevel();
            final int opHitpoints = stats.getHitpoints().getLevel();

            final JLabel comp = new JLabel(String.format("Attack: %d", deltaAttack()));
            comp.setFont(FontManager.getRunescapeFont());
            comparisonPanel.add(comp);
            JLabel comp1 = new JLabel(String.format("Strength: %d", deltaStrength()));
            comparisonPanel.add(comp1);
            JLabel comp2 = new JLabel(String.format("Defence: %d", deltaDefence()));
            comp2.setFont(FontManager.getRunescapeFont());
            comparisonPanel.add(comp2);
            JLabel comp3 = new JLabel(String.format("Hitpoints: %d", deltaHitpoints()));
            comp3.setFont(FontManager.getRunescapeFont());
            comparisonPanel.add(comp3);

            JLabel total = new JLabel("Total: " + deltaTotal());
            total.setFont(FontManager.getRunescapeBoldFont());
            if (deltaTotal() > 0) {
                total.setForeground(Color.yellow);
            } else if (deltaTotal() >= config.goodStake()) {
                total.setForeground(Color.green);
            } else {
                total.setForeground(Color.red);
            }
            comparisonPanel.add(total);

            JLabel odds = new JLabel("Odds: " + getOdds());
            odds.setFont(FontManager.getRunescapeBoldFont());
            if (getOdds() > 0) {
                odds.setForeground(Color.yellow);
            } else if (getOdds() >= config.goodStake()) {
                odds.setForeground(Color.green);
            } else {
                odds.setForeground(Color.red);
            }
            comparisonPanel.add(odds);
        } else {
            comparisonPanel.add(new JLabel("Stats not loaded"));
        }
        comparisonPanel.setVisible(true);

        revalidate();
        repaint();
    }
}
