package com.ellyzhia.cobolmod.client;

import com.ellyzhia.cobolmod.network.RaidActionPayload;
import com.ellyzhia.cobolmod.raid.RaidInstance;
import com.ellyzhia.cobolmod.raid.RaidType;
import com.ellyzhia.cobolmod.raid.RaidTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Petite fenêtre "Prêt ?" qui s'ouvre chez chaque membre quand le leader
 * demande la téléportation. Répondre Prêt ou Pas prêt l'envoie au serveur.
 */
public class ReadyCheckScreen extends Screen {
    private static final int W = 220, H = 90;
    private final RaidInstance raid;
    private final RaidType type;

    public ReadyCheckScreen(RaidInstance raid) {
        super(Component.translatable("screen.cobolmod.ready.title"));
        this.raid = raid;
        this.type = RaidTypes.byId(raid.typeId()).orElseThrow();
    }

    @Override
    protected void init() {
        int x = (width - W) / 2, y = (height - H) / 2;
        int bw = 90, bh = 20, by = y + H - bh - 10;
        addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.ready.yes"), b -> answer(true))
                .bounds(x + W / 2 - bw - 5, by, bw, bh).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.cobolmod.ready.no"), b -> answer(false))
                .bounds(x + W / 2 + 5, by, bw, bh).build());
    }

    private void answer(boolean ready) {
        PacketDistributor.sendToServer(new RaidActionPayload(
                RaidActionPayload.Action.READY_ANSWER, raid.typeId(), ready ? 1 : 0, false));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = (width - W) / 2, y = (height - H) / 2;
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF3A4660);
        g.fill(x, y, x + W, y + H, 0xF0161A26);
        g.fill(x, y, x + W, y + 18, 0xFFE53935);
        g.drawCenteredString(font, title, x + W / 2, y + 5, 0xFFECEFF4);
        g.drawCenteredString(font, Component.translatable("screen.cobolmod.ready.question", raid.leader().name(), type.name()),
                x + W / 2, y + 28, 0xFFECEFF4);
        g.drawCenteredString(font, Component.translatable("screen.cobolmod.ready.count",
                raid.readyCheck().size(), raid.members().size()), x + W / 2, y + 42, 0xFF9AA3B5);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // on force une réponse
    }
}
