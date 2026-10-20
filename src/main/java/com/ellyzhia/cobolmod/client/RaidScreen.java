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
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

/**
 * Écran des raids — version épurée :
 * bandeau rouge, liste des raids à gauche, fiche à droite avec le nom,
 * le boss, un badge de difficulté en haut à droite, une grande bannière
 * du boss, une ligne d'état/équipe et deux boutons.
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
    private static final int TEAL = 0xFF4DD0C4;
    private static final int BORDER = 0xFF3A4660;
    private static final int SEPARATOR = 0xFF3F4A78;
    private static final int GREEN = 0xFF66BB6A;
    private static final int ORANGE = 0xFFFFA726;
    private static final int RED = 0xFFEF5350;

    // --- Géométrie (unités GUI) ---
    private static final int HEADER_H = 24;
    private static final int LIST_W = 136;
    private static final int ROW_H = 30;
    private static final int PAD = 8;
    private static final int BTN_H = 20;
    private static final int BANNER_TEX_W = 256, BANNER_TEX_H = 96;

    private enum Tab { RAIDS, ADMIN }

    private Tab tab = Tab.RAIDS;
    private int selected = 0;
    private RaidDifficulty chosenDifficulty = RaidDifficulty.NORMAL;
    private boolean chosenPublic = true;

    private int panelX, panelY, panelW, panelH;
    private int listX, listY, listW, listH;
    private int cardX, cardY, cardW, cardH;
    private int badgeX, badgeY, badgeW, badgeH;

    public RaidScreen() {
        super(Component.translatable("screen.cobolmod.raids.title"));
    }

    // ------------------------------------------------------------------ init

    @Override
    protected void init() {
        panelW = Math.min(width - 20, 470);
        panelH = Math.min(height - 20, 256);
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

    private void buildWidgets() {
        clearWidgets();

        // Onglets dans le bandeau, à droite
        int tabW = 50, tabY = panelY + 4, tabH = HEADER_H - 8;
        int tabX = panelX + panelW - PAD - tabW;
        if (isAdmin()) {
            Button admin = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.tab.admin"),
                    b -> switchTab(Tab.ADMIN)).bounds(tabX, tabY, tabW, tabH).build());
            admin.active = tab != Tab.ADMIN;
            tabX -= tabW + 4;
        } else if (tab == Tab.ADMIN) {
            tab = Tab.RAIDS;
        }
        Button raids = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.tab.raids"),
                b -> switchTab(Tab.RAIDS)).bounds(tabX, tabY, tabW, tabH).build());
        raids.active = tab != Tab.RAIDS;

        // Deux boutons en bas de la fiche
        int btnY = cardY + cardH - PAD - BTN_H;
        int btnW = (cardW - 2 * PAD - 10) / 2;
        int b1 = cardX + PAD, b2 = b1 + btnW + 10;

        RaidType type = currentType();
        Optional<RaidInstance> inst = currentInstance();
        UUID me = me();
        Optional<RaidInstance> myRaid = ClientRaidState.raidOf(me);

        if (tab == Tab.ADMIN) {
            buildAdminButtons(type, inst, b1, b2, btnW, btnY);
            return;
        }

        if (inst.isEmpty()) {
            Button create = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.create"),
                    b -> createRaid()).bounds(b2, btnY, btnW, BTN_H).build());
            if (myRaid.isPresent()) {
                create.active = false;
                create.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.already_in_raid")));
            }
            Button vis = addRenderableWidget(Button.builder(visibilityChoiceLabel(), b -> {
                chosenPublic = !chosenPublic;
                b.setMessage(visibilityChoiceLabel());
            }).bounds(b1, btnY, btnW, BTN_H).build());
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
            Button create = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.create"),
                    b -> {}).bounds(b2, btnY, btnW, BTN_H).build());
            create.active = false;
            create.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.already_open")));
            return;
        }

        if (leader) {
            Button tp = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.teleport"),
                    b -> send(Action.REQUEST_TELEPORT, type.id())).bounds(b1, btnY, btnW, BTN_H).build());
            tp.setTooltip(Tooltip.create(Component.translatable("screen.cobolmod.raids.teleport.tip")));
            tp.active = raid.state() == RaidInstance.State.OPEN;
            addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.disband"),
                    b -> send(Action.LEAVE, type.id())).bounds(b2, btnY, btnW, BTN_H).build());
        } else if (raid.state() == RaidInstance.State.READY_CHECK && !raid.isReady(me)) {
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

    private void buildAdminButtons(RaidType type, Optional<RaidInstance> inst, int b1, int b2, int btnW, int btnY) {
        if (inst.isEmpty()) {
            Button none = addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.raids.admin.no_instance"),
                    b -> {}).bounds(b1, btnY, btnW * 2 + 10, BTN_H).build());
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

    private void createRaid() {
        PacketDistributor.sendToServer(new RaidActionPayload(Action.OPEN, currentType().id(),
                chosenDifficulty.ordinal(), chosenPublic));
    }

    private void answerReady(String typeId, boolean ready) {
        PacketDistributor.sendToServer(new RaidActionPayload(Action.READY_ANSWER, typeId, ready ? 1 : 0, false));
    }

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

        // Panneau + bandeau rouge
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, BORDER);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_PANEL);
        g.fill(panelX, panelY, panelX + panelW, panelY + HEADER_H, ACCENT);
        g.fill(panelX, panelY + HEADER_H - 3, panelX + panelW, panelY + HEADER_H, ACCENT_DARK);
        g.drawString(font, title, panelX + PAD, panelY + (HEADER_H - 8) / 2 - 1, TEXT, true);

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
                g.fill(listX, rowY, listX + 4, rowY + ROW_H, ACCENT);
            } else if (hovered) {
                g.fill(listX, rowY, listX + listW, rowY + ROW_H, ROW_HOVER);
            }

            // Nom en haut ; état discret en dessous (rien si fermé)
            g.drawString(font, font.plainSubstrByWidth(type.name(), listW - 20), listX + 12, rowY + 7, TEXT, true);
            if (inst.isPresent()) {
                RaidInstance r = inst.get();
                int color = switch (r.state()) {
                    case OPEN -> r.isFull() ? RED : GREEN;
                    case READY_CHECK -> ORANGE;
                    case IN_PROGRESS -> RED;
                };
                g.drawString(font, r.members().size() + "/" + r.maxPlayers() + " · " + r.difficulty().label(),
                        listX + 12, rowY + 17, color, false);
            }
        }
    }

    private void renderCard(GuiGraphics g, int mouseX, int mouseY) {
        RaidType type = currentType();
        Optional<RaidInstance> inst = currentInstance();
        RaidDifficulty diff = displayedDifficulty();
        int x = cardX + PAD;
        int y = cardY + PAD;
        int innerW = cardW - 2 * PAD;

        // Nom + boss
        g.drawString(font, type.name(), x, y, TEXT, true);
        int lvl = diff.scaledLevel(type.baseLevel());
        g.drawString(font, Component.translatable("screen.cobolmod.raids.boss", type.boss(), lvl), x, y + 12, TEAL, false);

        // Badge de difficulté, en haut à droite, cliquable avant ouverture (ou en admin)
        String label = diff.label();
        badgeW = font.width(label) + 12;
        badgeH = 12;
        badgeX = x + innerW - badgeW;
        badgeY = y - 1;
        boolean clickable = difficultyClickable();
        boolean hover = clickable && mouseX >= badgeX && mouseX < badgeX + badgeW && mouseY >= badgeY && mouseY < badgeY + badgeH;
        int badgeColor = 0xFF000000 | diff.color();
        g.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, hover ? lighten(badgeColor) : badgeColor);
        g.drawString(font, label, badgeX + 6, badgeY + 2, 0xFF101010, false);
        if (clickable) {
            g.renderOutline(badgeX - 1, badgeY - 1, badgeW + 2, badgeH + 2, hover ? TEXT : BORDER);
            if (hover) g.renderTooltip(font, Component.translatable("screen.cobolmod.raids.difficulty.tip"), mouseX, mouseY);
        }

        // Trait de séparation
        int sepY = y + 28;
        g.fill(x, sepY, x + innerW, sepY + 2, SEPARATOR);

        // Grande bannière du boss, pleine largeur, ratio 256:96
        int bannerY = sepY + 8;
        int bannerH = innerW * BANNER_TEX_H / BANNER_TEX_W;
        int maxBannerH = (cardY + cardH - PAD - BTN_H - 18) - bannerY;
        int bannerW = innerW;
        if (bannerH > maxBannerH) {
            bannerH = maxBannerH;
            bannerW = bannerH * BANNER_TEX_W / BANNER_TEX_H;
        }
        int bannerX = x + (innerW - bannerW) / 2;
        g.fill(bannerX - 1, bannerY - 1, bannerX + bannerW + 1, bannerY + bannerH + 1, 0xFF000000);
        g.blit(bossTexture(type), bannerX, bannerY, bannerW, bannerH, 0, 0, BANNER_TEX_W, BANNER_TEX_H, BANNER_TEX_W, BANNER_TEX_H);

        // Ligne d'état / équipe juste au-dessus des boutons
        int infoY = cardY + cardH - PAD - BTN_H - 12;
        if (inst.isPresent()) {
            RaidInstance raid = inst.get();
            int stateColor = switch (raid.state()) {
                case OPEN -> GREEN;
                case READY_CHECK -> ORANGE;
                case IN_PROGRESS -> RED;
            };
            Component state = Component.translatable("screen.cobolmod.raids.state." + raid.state().name().toLowerCase());
            g.drawString(font, state, x, infoY, stateColor, false);
            int tx = x + font.width(state) + 6;
            g.drawString(font, "·", tx, infoY, TEXT_DIM, false);
            tx += 8;
            StringBuilder team = new StringBuilder();
            for (RaidInstance.RaidMember m : raid.members()) {
                if (team.length() > 0) team.append(", ");
                if (raid.isLeader(m.uuid())) team.append("★");
                team.append(m.name());
                if (raid.state() == RaidInstance.State.READY_CHECK) team.append(raid.isReady(m.uuid()) ? "✔" : "…");
            }
            String right = (raid.isPublic() ? "" : "Privé · ") + raid.members().size() + "/" + raid.maxPlayers();
            int rightW = font.width(right);
            g.drawString(font, font.plainSubstrByWidth(team.toString(), x + innerW - rightW - 6 - tx), tx, infoY, TEXT_DIM, false);
            g.drawString(font, right, x + innerW - rightW, infoY, raid.isPublic() ? TEXT_DIM : ORANGE, false);
        } else {
            g.drawString(font, Component.translatable("screen.cobolmod.raids.state.closed"), x, infoY, TEXT_DIM, false);
            if (tab == Tab.ADMIN) {
                Component hint = Component.translatable("screen.cobolmod.raids.admin.hint");
                g.drawString(font, hint, x + innerW - font.width(hint), infoY, ORANGE, false);
            }
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
