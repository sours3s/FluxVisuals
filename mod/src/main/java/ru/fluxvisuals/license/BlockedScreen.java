package ru.fluxvisuals.license;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Экран-заглушка: показывается, если мод запущен не через лоадер FluxVisuals
 * (нет валидного launch-тикета). Визуалы в этом случае не работают.
 */
@Environment(EnvType.CLIENT)
public class BlockedScreen extends Screen {
   public BlockedScreen() {
      super(Text.literal("FluxVisuals"));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context, mouseX, mouseY, delta);
      int cx = this.width / 2;
      context.drawCenteredTextWithShadow(this.textRenderer, "FluxVisuals недоступен", cx, this.height / 2 - 14, 0xFFA855F7);
      context.drawCenteredTextWithShadow(this.textRenderer, "Клиент запускается только через лоадер FluxVisuals", cx, this.height / 2 + 4, 0xFF8B949E);
      super.render(context, mouseX, mouseY, delta);
   }
}
