package com.ellyzhia.cobolmod.client;

import com.ellyzhia.cobolmod.raid.RaidDifficulty;
import com.ellyzhia.cobolmod.raid.RaidInfo;
import com.ellyzhia.cobolmod.raid.RaidRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Écran des raids, design "Pokédex moderne" :
 * panneau sombre, bandeau d'en-tête rouge, liste des raids à gauche,
 * fiche détaillée à droite. Onglet Admin visible uniquement pour les opérateurs.
 * <p>
 * Ouvert avec la touche U (voir {@link ModKeyBindings}).
 */
public class RaidScreen extends Screen {
    // --- Palette ---
    private static final int BG_PANEL = 0xF0161A26;      // fond principal (bleu nuit)
    private static final int BG_LIST = 0xFF1E2434;       // colonne de gauche
    private static final int BG_CARD = 0xFF222A3C;       // fiche de droite
    private static final int ACCENT = 0xFFE53935;        // rouge Pokédex
    private static final int ACCENT_DARK = 0xFFB71C1C;
    private static final int ROW_HOVER = 0xFF2C3650;
    private static final int ROW_SELECTED = 0xFF34405E;
    private static final int TEXT = 0xFFECEFF4;
    private static final int TEXT_DIM = 0xFF9AA3B5;
    private static final int BORDER = 0xFF3A4660;

    // --- Géométrie ---
    private static final int HEADER_H = 22;
    private static final int LIST_W = 130;
    private static final int ROW_H = 24;
    private static final int PAD = 8;

    private enum Tab { RAIDS, ADMIN }

    private Tab tab = Tab.RAIDS;
    private int selected = 0;

    private int panelX, panelY, panelW, panelH;
    private int listX, listY, listW, listH;
    private int cardX, cardY, cardW, cardH;

    // Boutons recréés à chaque init()/refresh()
    private Button joinButton;
    private Button teleportButton;
    private Button difficultyButton;
    private Button visibilityButton;
    private Button tabRaidsButton;
    private Button tabAdminButton;

    public RaidScreen() {
        super(Component.translatable("screen.cobolmod.raids.title"));
    }

    // ------------------------------------------------------------------ init

    @Override
    protected void init() {
        panelW = Math.min(width - 24, 420);
        panelH = Math.min(height - 24, 230);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        listX = panelX + PAD;
        listY = panelY + HEADER_H + PAD;
        listW = LIST_W;
        listH = panelH - HEADER_H - 2 * PAD;

        cardX = listX + listW + PAD;
        cardY = listY;
        cardW = panelX + panelW - PAD - cardX;
        cardH = listH;

        selected = Math.min(selected, RaidRegistry.size() - 1);
        buildWidgets();
    }

    private boolean isAdmin() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    /** (Re)crée tous les boutons selon l'onglet courant et le raid sélectionné. */
    private void buildWidgets() {
        clearWidgets();

        // Onglets, dans l'en-tête à droite
        int tabW = 52;
        int tabY = panelY + 3;
        int tabX = panelX + panelW - PAD - tabW;
        if (isAdmin()) {
            tabAdminButton = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.tab.admin"),
                    b -> switchTab(Tab.ADMIN)).bounds(tabX, tabY, tabW, HEADER_H - 6).build());
            tabAdminButton.active = tab != Tab.ADMIN;
            tabX -= tabW + 4;
        } else if (tab == Tab.ADMIN) {
            tab = Tab.RAIDS; // sécurité : on a perdu les droits
        }
        tabRaidsButton = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.tab.raids"),
                b -> switchTab(Tab.RAIDS)).bounds(tabX, tabY, tabW, HEADER_H - 6).build());
        tabRaidsButton.active = tab != Tab.RAIDS;

        // Bouton fermer (coin supérieur gauche de l'en-tête, après le titre)
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(panelX + 4, panelY + 3, 16, HEADER_H - 6).build());

        // Boutons d'action en bas de la fiche
        int btnH = 20;
        int btnY = cardY + cardH - PAD - btnH;
        int btnW = (cardW - 2 * PAD - 6) / 2;
        int btn1X = cardX + PAD;
        int btn2X = btn1X + btnW + 6;

        RaidInfo raid = current();

        if (tab == Tab.RAIDS) {
            joinButton = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.join"),
                    b -> joinRaid()).bounds(btn1X, btnY, btnW, btnH).build());
            joinButton.active = !raid.isFull();
            if (raid.isFull()) {
                joinButton.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.full")));
            } else if (!raid.isPublic()) {
                joinButton.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.invite_only")));
            }

            teleportButton = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.teleport"),
                    b -> teleport()).bounds(btn2X, btnY, btnW, btnH).build());
        } else {
            difficultyButton = addRenderableWidget(Button.builder(difficultyLabel(raid),
                    b -> cycleDifficulty()).bounds(btn1X, btnY, btnW, btnH).build());
            visibilityButton = addRenderableWidget(Button.builder(visibilityLabel(raid),
                    b -> toggleVisibility()).bounds(btn2X, btnY, btnW, btnH).build());
        }
    }

    private void switchTab(Tab newTab) {
        tab = newTab;
        buildWidgets();
    }

    private RaidInfo current() {
        return RaidRegistry.get(selected);
    }

    // --------------------------------------------------------------- actions

    private void joinRaid() {
        RaidInfo raid = current();
        if (minecraft == null || minecraft.player == null) return;
        // Provisoire : le vrai "rejoindre" passera par un paquet vers le serveur.
        minecraft.player.displayClientMessage(
                Component.translatable("screen.cobolmod.raids.msg.join", raid.name()), false);
        onClose();
    }

    private void teleport() {
        RaidInfo raid = current();
        if (minecraft == null || minecraft.player == null) return;
        // Provisoire : la téléportation réelle sera faite côté serveur.
        minecraft.player.displayClientMessage(
                Component.translatable("screen.cobolmod.raids.msg.teleport", raid.name()), false);
        onClose();
    }

    private void cycleDifficulty() {
        RaidInfo raid = current().withDifficulty(current().difficulty().next());
        RaidRegistry.replace(selected, raid);
        difficultyButton.setMessage(difficultyLabel(raid));
    }

    private void toggleVisibility() {
        RaidInfo raid = current().withPublic(!current().isPublic());
        RaidRegistry.replace(selected, raid);
        visibilityButton.setMessage(visibilityLabel(raid));
    }

    private static Component difficultyLabel(RaidInfo raid) {
        return Component.translatable("screen.cobolmod.raids.admin.difficulty", raid.difficulty().label());
    }

    private static Component visibilityLabel(RaidInfo raid) {
        return Component.translatable(raid.isPublic()
                ? "screen.cobolmod.raids.admin.make_private"
                : "screen.cobolmod.raids.admin.make_public");
    }

    // ---------------------------------------------------------------- souris

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
            int index = (int) ((mouseY - listY) / ROW_H);
            if (index >= 0 && index < RaidRegistry.size() && index != selected) {
                selected = index;
                buildWidgets();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // La touche U referme aussi l'écran
        if (ModKeyBindings.OPEN_RAIDS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ----------------------------------------------------------------- rendu

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick); // assombrit / floute le jeu

        // Panneau principal + bordure
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, BORDER);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_PANEL);

        // En-tête rouge style Pokédex, avec liseré sombre
        g.fill(panelX, panelY, panelX + panelW, panelY + HEADER_H, ACCENT);
        g.fill(panelX, panelY + HEADER_H - 2, panelX + panelW, panelY + HEADER_H, ACCENT_DARK);
        // "Diode" bleue du Pokédex à côté du titre
        int ledX = panelX + 26, ledY = panelY + HEADER_H / 2;
        g.fill(ledX - 4, ledY - 4, ledX + 4, ledY + 4, 0xFF0D47A1);
        g.fill(ledX - 3, ledY - 3, ledX + 3, ledY + 3, 0xFF42A5F5);
        g.fill(ledX - 2, ledY - 3, ledX, ledY - 1, 0xFFBBDEFB);
        g.drawString(font, title, ledX + 8, panelY + (HEADER_H - 8) / 2, TEXT, true);

        // Colonne liste
        g.fill(listX, listY, listX + listW, listY + listH, BG_LIST);
        renderList(g, mouseX, mouseY);

        // Fiche
        g.fill(cardX, cardY, cardX + cardW, cardY + cardH, BG_CARD);
        renderCard(g);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        List<RaidInfo> raids = RaidRegistry.all();
        boolean hoverInList = mouseX >= listX && mouseX < listX + listW;
        for (int i = 0; i < raids.size(); i++) {
            int rowY = listY + i * ROW_H;
            if (rowY + ROW_H > listY + listH) break;

            RaidInfo raid = raids.get(i);
            boolean hovered = hoverInList && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (i == selected) {
                g.fill(listX, rowY, listX + listW, rowY + ROW_H, ROW_SELECTED);
                g.fill(listX, rowY, listX + 3, rowY + ROW_H, ACCENT); // marqueur rouge
            } else if (hovered) {
                g.fill(listX, rowY, listX + listW, rowY + ROW_H, ROW_HOVER);
            }

            // Pastille de difficulté
            int dotX = listX + 10, dotY = rowY + ROW_H / 2;
            g.fill(dotX - 3, dotY - 3, dotX + 3, dotY + 3, 0xFF000000 | raid.difficulty().color());

            String name = font.plainSubstrByWidth(raid.name(), listW - 40);
            g.drawString(font, name, listX + 18, rowY + 4, TEXT, false);
            String sub = raid.players() + "/" + raid.maxPlayers() + (raid.isPublic() ? "" : " · privé");
            g.drawString(font, sub, listX + 18, rowY + 13, TEXT_DIM, false);

            // Séparateur
            g.fill(listX + 4, rowY + ROW_H - 1, listX + listW - 4, rowY + ROW_H, BORDER);
        }
    }

    private void renderCard(GuiGraphics g) {
        RaidInfo raid = current();
        int x = cardX + PAD;
        int y = cardY + PAD;
        int innerW = cardW - 2 * PAD;

        // Nom du raid + badge difficulté sur la même ligne
        g.drawString(font, raid.name(), x, y, TEXT, true);
        String diff = raid.difficulty().label().toUpperCase();
        int badgeW = font.width(diff) + 8;
        int badgeX = cardX + cardW - PAD - badgeW;
        g.fill(badgeX, y - 2, badgeX + badgeW, y + 10, 0xFF000000 | raid.difficulty().color());
        g.drawString(font, diff, badgeX + 4, y, 0xFF101010, false);
        y += 13;

        // Boss
        g.drawString(font, Component.translatable("screen.cobolmod.raids.boss", raid.boss(), raid.bossLevel()),
                x, y, TEXT_DIM, false);
        y += 12;

        // Joueurs + visibilité
        String players = raid.players() + " / " + raid.maxPlayers();
        g.drawString(font, Component.translatable("screen.cobolmod.raids.players", players), x, y,
                raid.isFull() ? 0xFFEF5350 : TEXT_DIM, false);
        Component visibility = Component.translatable(raid.isPublic()
                ? "screen.cobolmod.raids.public" : "screen.cobolmod.raids.private");
        g.drawString(font, visibility, cardX + cardW - PAD - font.width(visibility), y,
                raid.isPublic() ? 0xFF66BB6A : 0xFFFFA726, false);
        y += 14;

        // Jauge de remplissage
        int barH = 4;
        g.fill(x, y, x + innerW, y + barH, 0xFF0F131C);
        int fillW = raid.maxPlayers() == 0 ? 0 : innerW * raid.players() / raid.maxPlayers();
        g.fill(x, y, x + fillW, y + barH, raid.isFull() ? 0xFFEF5350 : ACCENT);
        y += barH + 8;

        // Séparateur
        g.fill(x, y, x + innerW, y + 1, BORDER);
        y += 6;

        // Zone "image" du boss : placeholder encadré en attendant les sprites Cobblemon
        int imgSize = 40;
        g.fill(x, y, x + imgSize, y + imgSize, 0xFF0F131C);
        g.renderOutline(x, y, imgSize, imgSize, BORDER);
        String initial = raid.boss().isEmpty() ? "?" : raid.boss().substring(0, 1);
        g.drawCenteredString(font, initial, x + imgSize / 2, y + imgSize / 2 - 4, TEXT_DIM);

        // Description, à droite de l'image, coupée en lignes
        int descX = x + imgSize + 6;
        int descW = innerW - imgSize - 6;
        int maxLines = 4;
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(raid.description()), descW);
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            g.drawString(font, lines.get(i), descX, y + i * 10, TEXT_DIM, false);
        }

        // Rappel de l'onglet admin
        if (tab == Tab.ADMIN) {
            Component hint = Component.translatable("screen.cobolmod.raids.admin.hint");
            g.drawString(font, hint, x, cardY + cardH - PAD - 20 - 12, 0xFFFFA726, false);
        }
    }
}
