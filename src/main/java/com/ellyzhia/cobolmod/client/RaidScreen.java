package com.ellyzhia.cobolmod.client;

import com.ellyzhia.cobolmod.CobolMod;
import com.ellyzhia.cobolmod.network.RaidActionPayload;
import com.ellyzhia.cobolmod.network.RaidActionPayload.Action;
import com.ellyzhia.cobolmod.raid.RaidDifficulty;
import com.ellyzhia.cobolmod.raid.RaidInstance;
import com.ellyzhia.cobolmod.raid.RaidType;
import com.ellyzhia.cobolmod.raid.RaidTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Écran des raids, design "Pokédex moderne".
 * <p>
 * Gauche : liste des types de raid (avec l'état de l'instance s'il y en a une).
 * Droite : grande image du boss, badge de difficulté cliquable, équipe, actions.
 * Onglet Admin pour les opérateurs.
 * <p>
 * Ouvert avec la touche U (voir {@link ModKeyBindings}).
 */
public class RaidScreen extends Screen {
    // --- Palette ---
    private static final int BG_PANEL = 0xF0161A26;
    private static final int BG_LIST = 0xFF1E2434;
    private static final int BG_CARD = 0xFF222A3C;
    private static final int ACCENT = 0xFFE53935;
    private static final int ACCENT_DARK = 0xFFB71C1C;
    private static final int ROW_HOVER = 0xFF2C3650;
    private static final int ROW_SELECTED = 0xFF34405E;
    private static final int TEXT = 0xFFECEFF4;
    private static final int TEXT_DIM = 0xFF9AA3B5;
    private static final int BORDER = 0xFF3A4660;
    private static final int GREEN = 0xFF66BB6A;
    private static final int ORANGE = 0xFFFFA726;
    private static final int RED = 0xFFEF5350;

    // --- Géométrie ---
    private static final int HEADER_H = 22;
    private static final int LIST_W = 120;
    private static final int ROW_H = 24;
    private static final int PAD = 8;
    private static final int IMG_SIZE = 96;
    private static final int BTN_H = 20;

    private enum Tab { RAIDS, ADMIN }

    private Tab tab = Tab.RAIDS;
    private int selected = 0;
    /** Difficulté choisie pour l'ouverture (quand aucune instance n'existe). */
    private RaidDifficulty chosenDifficulty = RaidDifficulty.NORMAL;
    private boolean chosenPublic = true;

    private int panelX, panelY, panelW, panelH;
    private int listX, listY, listW, listH;
    private int cardX, cardY, cardW, cardH;
    /** Rectangle du badge de difficulté (cliquable). */
    private int badgeX, badgeY, badgeW, badgeH;

    public RaidScreen() {
        super(Component.translatable("screen.cobolmod.raids.title"));
    }

    // ------------------------------------------------------------------ init

    @Override
    protected void init() {
        panelW = Math.min(width - 24, 440);
        panelH = Math.min(height - 24, 250);
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

        selected = Math.max(0, Math.min(selected, RaidTypes.ALL.size() - 1));
        buildWidgets();
    }

    /** Appelé par ClientRaidState quand le serveur envoie un nouvel état. */
    public void onStateUpdated() {
        buildWidgets();
    }

    private boolean isAdmin() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    private UUID me() {
        return minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : new UUID(0, 0);
    }

    private RaidType currentType() {
        return RaidTypes.ALL.get(selected);
    }

    private Optional<RaidInstance> currentInstance() {
        return ClientRaidState.get(currentType().id());
    }

    private static void send(Action action, String typeId) {
        PacketDistributor.sendToServer(RaidActionPayload.of(action, typeId));
    }

    /** (Re)crée tous les boutons selon l'onglet courant, le raid sélectionné et son état. */
    private void buildWidgets() {
        clearWidgets();

        // --- En-tête : fermer + onglets ---
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose())
                .bounds(panelX + 4, panelY + 3, 16, HEADER_H - 6).build());

        int tabW = 52, tabY = panelY + 3;
        int tabX = panelX + panelW - PAD - tabW;
        if (isAdmin()) {
            Button admin = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.tab.admin"),
                    b -> switchTab(Tab.ADMIN)).bounds(tabX, tabY, tabW, HEADER_H - 6).build());
            admin.active = tab != Tab.ADMIN;
            tabX -= tabW + 4;
        } else if (tab == Tab.ADMIN) {
            tab = Tab.RAIDS;
        }
        Button raids = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.tab.raids"),
                b -> switchTab(Tab.RAIDS)).bounds(tabX, tabY, tabW, HEADER_H - 6).build());
        raids.active = tab != Tab.RAIDS;

        // --- Boutons d'action, en bas de la fiche ---
        int btnY = cardY + cardH - PAD - BTN_H;
        int btnW = (cardW - 2 * PAD - 6) / 2;
        int b1 = cardX + PAD, b2 = b1 + btnW + 6;

        RaidType type = currentType();
        Optional<RaidInstance> inst = currentInstance();
        UUID me = me();
        Optional<RaidInstance> myRaid = ClientRaidState.raidOf(me);

        if (tab == Tab.ADMIN) {
            buildAdminButtons(type, inst, b1, b2, btnW, btnY);
            return;
        }

        if (inst.isEmpty()) {
            // Aucune instance : on peut en ouvrir une (sauf si déjà dans un autre raid)
            Button open = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.open"),
                    b -> openRaid()).bounds(b1, btnY, btnW, BTN_H).build());
            if (myRaid.isPresent()) {
                open.active = false;
                open.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.already_in_raid")));
            }
            Button vis = addRenderableWidget(Button.builder(visibilityChoiceLabel(), b -> {
                chosenPublic = !chosenPublic;
                b.setMessage(visibilityChoiceLabel());
            }).bounds(b2, btnY, btnW, BTN_H).build());
            vis.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.visibility.tip")));
            return;
        }

        RaidInstance raid = inst.get();
        boolean member = raid.isMember(me);
        boolean leader = raid.isLeader(me);

        if (!member) {
            Button join = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.join"),
                    b -> send(Action.JOIN, type.id())).bounds(b1, btnY, btnW, BTN_H).build());
            if (raid.isFull()) {
                join.active = false;
                join.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.full")));
            } else if (raid.state() != RaidInstance.State.OPEN) {
                join.active = false;
                join.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.in_progress")));
            } else if (!raid.isPublic() && !isAdmin()) {
                join.active = false;
                join.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.invite_only")));
            } else if (myRaid.isPresent()) {
                join.active = false;
                join.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.already_in_raid")));
            }
            return;
        }

        // Membre de ce raid
        if (leader) {
            Button tp = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.teleport"),
                    b -> send(Action.REQUEST_TELEPORT, type.id())).bounds(b1, btnY, btnW, BTN_H).build());
            tp.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.teleport.tip")));
            tp.active = raid.state() == RaidInstance.State.OPEN;
            addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.disband"),
                    b -> send(Action.LEAVE, type.id())).bounds(b2, btnY, btnW, BTN_H).build());
        } else {
            if (raid.state() == RaidInstance.State.READY_CHECK && !raid.isReady(me)) {
                addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.ready.yes"),
                        b -> answerReady(type.id(), true)).bounds(b1, btnY, btnW, BTN_H).build());
                addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.ready.no"),
                        b -> answerReady(type.id(), false)).bounds(b2, btnY, btnW, BTN_H).build());
            } else {
                Button wait = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.waiting_leader"),
                        b -> {}).bounds(b1, btnY, btnW, BTN_H).build());
                wait.active = false;
                addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.leave"),
                        b -> send(Action.LEAVE, type.id())).bounds(b2, btnY, btnW, BTN_H).build());
            }
        }
    }

    private void buildAdminButtons(RaidType type, Optional<RaidInstance> inst, int b1, int b2, int btnW, int btnY) {
        if (inst.isEmpty()) {
            Button none = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.admin.no_instance"),
                    b -> {}).bounds(b1, btnY, btnW * 2 + 6, BTN_H).build());
            none.active = false;
            return;
        }
        RaidInstance raid = inst.get();
        Button vis = addRenderableWidget(Button.builder(
                Component.translatable(raid.isPublic() ? "screen.cobolmod.raids.admin.make_private"
                        : "screen.cobolmod.raids.admin.make_public"),
                b -> PacketDistributor.sendToServer(new RaidActionPayload(Action.ADMIN_PUBLIC, type.id(), 0, !raid.isPublic())))
                .bounds(b1, btnY, btnW, BTN_H).build());
        vis.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.admin.tip")));
        addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.admin.close"),
                b -> send(Action.ADMIN_CLOSE, type.id())).bounds(b2, btnY, btnW, BTN_H).build());
    }

    private Component visibilityChoiceLabel() {
        return Component.translatable(chosenPublic ? "screen.cobolmod.raids.visibility.public"
                : "screen.cobolmod.raids.visibility.private");
    }

    private void switchTab(Tab newTab) {
        tab = newTab;
        buildWidgets();
    }

    // --------------------------------------------------------------- actions

    private void openRaid() {
        PacketDistributor.sendToServer(new RaidActionPayload(Action.OPEN, currentType().id(),
                chosenDifficulty.ordinal(), chosenPublic));
    }

    private void answerReady(String typeId, boolean ready) {
        PacketDistributor.sendToServer(new RaidActionPayload(Action.READY_ANSWER, typeId, ready ? 1 : 0, false));
    }

    /** Clic sur le badge de difficulté : avant ouverture on cycle le choix ; en admin on change l'instance. */
    private void onDifficultyClicked() {
        Optional<RaidInstance> inst = currentInstance();
        if (inst.isEmpty()) {
            chosenDifficulty = chosenDifficulty.next();
        } else if (tab == Tab.ADMIN && inst.get().state() != RaidInstance.State.IN_PROGRESS) {
            PacketDistributor.sendToServer(new RaidActionPayload(Action.ADMIN_DIFFICULTY, currentType().id(),
                    inst.get().difficulty().next().ordinal(), false));
        }
    }

    private RaidDifficulty displayedDifficulty() {
        return currentInstance().map(RaidInstance::difficulty).orElse(chosenDifficulty);
    }

    private boolean difficultyClickable() {
        Optional<RaidInstance> inst = currentInstance();
        return inst.isEmpty() || (tab == Tab.ADMIN && inst.get().state() != RaidInstance.State.IN_PROGRESS);
    }

    // ---------------------------------------------------------------- souris

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                int index = (int) ((mouseY - listY) / ROW_H);
                if (index >= 0 && index < RaidTypes.ALL.size() && index != selected) {
                    selected = index;
                    buildWidgets();
                    return true;
                }
            }
            if (difficultyClickable() && mouseX >= badgeX && mouseX < badgeX + badgeW
                    && mouseY >= badgeY && mouseY < badgeY + badgeH) {
                onDifficultyClicked();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
        super.renderBackground(g, mouseX, mouseY, partialTick);

        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, BORDER);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_PANEL);

        // En-tête rouge style Pokédex + diode bleue
        g.fill(panelX, panelY, panelX + panelW, panelY + HEADER_H, ACCENT);
        g.fill(panelX, panelY + HEADER_H - 2, panelX + panelW, panelY + HEADER_H, ACCENT_DARK);
        int ledX = panelX + 26, ledY = panelY + HEADER_H / 2;
        g.fill(ledX - 4, ledY - 4, ledX + 4, ledY + 4, 0xFF0D47A1);
        g.fill(ledX - 3, ledY - 3, ledX + 3, ledY + 3, 0xFF42A5F5);
        g.fill(ledX - 2, ledY - 3, ledX, ledY - 1, 0xFFBBDEFB);
        g.drawString(font, title, ledX + 8, panelY + (HEADER_H - 8) / 2, TEXT, true);

        g.fill(listX, listY, listX + listW, listY + listH, BG_LIST);
        renderList(g, mouseX, mouseY);

        g.fill(cardX, cardY, cardX + cardW, cardY + cardH, BG_CARD);
        renderCard(g, mouseX, mouseY);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        boolean hoverInList = mouseX >= listX && mouseX < listX + listW;
        for (int i = 0; i < RaidTypes.ALL.size(); i++) {
            int rowY = listY + i * ROW_H;
            if (rowY + ROW_H > listY + listH) break;

            RaidType type = RaidTypes.ALL.get(i);
            Optional<RaidInstance> inst = ClientRaidState.get(type.id());
            boolean hovered = hoverInList && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (i == selected) {
                g.fill(listX, rowY, listX + listW, rowY + ROW_H, ROW_SELECTED);
                g.fill(listX, rowY, listX + 3, rowY + ROW_H, ACCENT);
            } else if (hovered) {
                g.fill(listX, rowY, listX + listW, rowY + ROW_H, ROW_HOVER);
            }

            // Pastille d'état : gris = fermé, vert = ouvert, orange = ready check, rouge = en cours
            int dotColor = inst.map(r -> switch (r.state()) {
                case OPEN -> r.isFull() ? RED : GREEN;
                case READY_CHECK -> ORANGE;
                case IN_PROGRESS -> RED;
            }).orElse(0xFF5A6478);
            int dotX = listX + 10, dotY = rowY + ROW_H / 2;
            g.fill(dotX - 3, dotY - 3, dotX + 3, dotY + 3, dotColor);

            g.drawString(font, font.plainSubstrByWidth(type.name(), listW - 24), listX + 18, rowY + 4, TEXT, false);
            String sub = inst.map(r -> r.members().size() + "/" + r.maxPlayers() + " · " + r.difficulty().label())
                    .orElse("Fermé");
            g.drawString(font, sub, listX + 18, rowY + 13, TEXT_DIM, false);
            g.fill(listX + 4, rowY + ROW_H - 1, listX + listW - 4, rowY + ROW_H, BORDER);
        }
    }

    private void renderCard(GuiGraphics g, int mouseX, int mouseY) {
        RaidType type = currentType();
        Optional<RaidInstance> inst = currentInstance();
        RaidDifficulty diff = displayedDifficulty();
        int x = cardX + PAD;
        int y = cardY + PAD;
        int innerW = cardW - 2 * PAD;

        // --- Grande image du boss, à gauche de la fiche ---
        int imgX = x, imgY = y;
        g.fill(imgX - 1, imgY - 1, imgX + IMG_SIZE + 1, imgY + IMG_SIZE + 1, BORDER);
        ResourceLocation tex = bossTexture(type);
        g.blit(tex, imgX, imgY, 0, 0, IMG_SIZE, IMG_SIZE, IMG_SIZE, IMG_SIZE);
        // Bandeau "niveau" sous l'image
        int lvl = diff.scaledLevel(type.baseLevel());
        g.fill(imgX, imgY + IMG_SIZE - 12, imgX + IMG_SIZE, imgY + IMG_SIZE, 0xA0000000);
        g.drawCenteredString(font, Component.translatable("screen.cobolmod.raids.boss", type.boss(), lvl),
                imgX + IMG_SIZE / 2, imgY + IMG_SIZE - 10, TEXT);

        // --- Colonne d'infos à droite de l'image ---
        int ix = imgX + IMG_SIZE + PAD;
        int iw = cardX + cardW - PAD - ix;
        int iy = y;

        g.drawString(font, type.name(), ix, iy, TEXT, true);
        iy += 12;

        // Badge de difficulté cliquable
        String label = diff.label().toUpperCase();
        badgeW = font.width(label) + 14;
        badgeH = 12;
        badgeX = ix;
        badgeY = iy;
        boolean clickable = difficultyClickable();
        boolean hover = clickable && mouseX >= badgeX && mouseX < badgeX + badgeW && mouseY >= badgeY && mouseY < badgeY + badgeH;
        int badgeColor = 0xFF000000 | diff.color();
        g.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, hover ? lighten(badgeColor) : badgeColor);
        if (clickable) g.renderOutline(badgeX - 1, badgeY - 1, badgeW + 2, badgeH + 2, hover ? TEXT : BORDER);
        g.drawString(font, label, badgeX + 4, badgeY + 2, 0xFF101010, false);
        if (clickable) g.drawString(font, "↻", badgeX + badgeW - 9, badgeY + 2, 0xFF101010, false);
        if (hover) {
            g.renderTooltip(font, Component.translatable("screen.cobolmod.raids.difficulty.tip"), mouseX, mouseY);
        }
        iy += badgeH + 6;

        // État + visibilité
        if (inst.isPresent()) {
            RaidInstance raid = inst.get();
            Component state = Component.translatable("screen.cobolmod.raids.state." + raid.state().name().toLowerCase());
            int stateColor = switch (raid.state()) {
                case OPEN -> GREEN;
                case READY_CHECK -> ORANGE;
                case IN_PROGRESS -> RED;
            };
            g.drawString(font, state, ix, iy, stateColor, false);
            Component vis = Component.translatable(raid.isPublic() ? "screen.cobolmod.raids.public" : "screen.cobolmod.raids.private");
            g.drawString(font, vis, ix + iw - font.width(vis), iy, raid.isPublic() ? GREEN : ORANGE, false);
            iy += 12;

            // Équipe : nom + coche si prêt pendant un ready check
            g.drawString(font, Component.translatable("screen.cobolmod.raids.team", raid.members().size(), raid.maxPlayers()),
                    ix, iy, TEXT_DIM, false);
            iy += 10;
            for (RaidInstance.RaidMember m : raid.members()) {
                boolean isLeader = raid.isLeader(m.uuid());
                String line = (isLeader ? "★ " : "• ") + m.name();
                int color = m.uuid().equals(me()) ? TEXT : TEXT_DIM;
                g.drawString(font, font.plainSubstrByWidth(line, iw - 12), ix + 2, iy, color, false);
                if (raid.state() == RaidInstance.State.READY_CHECK) {
                    boolean ready = raid.isReady(m.uuid());
                    g.drawString(font, ready ? "✔" : "…", ix + iw - 8, iy, ready ? GREEN : ORANGE, false);
                }
                iy += 10;
            }
            for (int i = raid.members().size(); i < raid.maxPlayers(); i++) {
                g.drawString(font, "• —", ix + 2, iy, 0xFF4A5468, false);
                iy += 10;
            }
        } else {
            g.drawString(font, Component.translatable("screen.cobolmod.raids.state.closed"), ix, iy, TEXT_DIM, false);
            iy += 12;
            List<FormattedCharSequence> lines = font.split(Component.literal(type.description()), iw);
            for (int i = 0; i < Math.min(lines.size(), 4); i++) {
                g.drawString(font, lines.get(i), ix, iy, TEXT_DIM, false);
                iy += 10;
            }
        }

        // Séparateur au-dessus des boutons
        int sepY = cardY + cardH - PAD - BTN_H - 6;
        g.fill(x, sepY, x + innerW, sepY + 1, BORDER);

        if (tab == Tab.ADMIN) {
            g.drawString(font, Component.translatable("screen.cobolmod.raids.admin.hint"), x, sepY - 10, ORANGE, false);
        }
    }

    private static int lighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 40);
        int gr = Math.min(255, ((argb >> 8) & 0xFF) + 40);
        int b = Math.min(255, (argb & 0xFF) + 40);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    private ResourceLocation bossTexture(RaidType type) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(CobolMod.MODID, "textures/gui/boss/" + type.bossTextureName() + ".png");
        if (minecraft != null && minecraft.getResourceManager().getResource(rl).isEmpty()) {
            return ResourceLocation.fromNamespaceAndPath(CobolMod.MODID, "textures/gui/boss/unknown.png");
        }
        return rl;
    }
}
