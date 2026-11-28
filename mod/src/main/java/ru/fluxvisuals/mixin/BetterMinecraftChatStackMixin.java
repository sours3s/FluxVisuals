package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.module.impl.visuals.BetterMinecraft;

import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Эталонное склеивание одинаковых сообщений чата. */
@Environment(EnvType.CLIENT)
@Mixin(ChatHud.class)
public abstract class BetterMinecraftChatStackMixin {
    @Shadow private List<ChatHudLine> messages;
    @Shadow private List<ChatHudLine.Visible> visibleMessages;

    @Unique private String fluxvisuals$pendingStack = null;
    @Unique private final HashMap<String, Integer> fluxvisuals$countMap = new HashMap<>();
    @Unique private static final Pattern fluxvisuals$STACK_PATTERN = Pattern.compile("^(.*?)\\s*\u00a77\\(x(\\d+)\\)$", Pattern.DOTALL);

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), cancellable = true)
    private void fluxvisuals$onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
       if (BetterMinecraft.INSTANCE.isEnabled() && BetterMinecraft.INSTANCE.stackMessages.get()) {
          if (fluxvisuals$tryStackMessage(message)) ci.cancel();
       }
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("TAIL"))
    private void fluxvisuals$onAddMessageTail(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
       if (fluxvisuals$pendingStack != null && messages != null && !messages.isEmpty()) {
          fluxvisuals$countMap.put(fluxvisuals$pendingStack, 1);
          fluxvisuals$pendingStack = null;
       }
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void fluxvisuals$onClear(boolean clearHistory, CallbackInfo ci) {
       fluxvisuals$countMap.clear();
       fluxvisuals$pendingStack = null;
    }

    @Unique
    private boolean fluxvisuals$tryStackMessage(Text message) {
       if (messages == null) return false;
       String incoming = fluxvisuals$stripStack(message.getString());
       if (incoming.isBlank()) return false;

       MinecraftClient mc = MinecraftClient.getInstance();
       if (mc == null || mc.inGameHud == null) return false;

       Integer currentCount = fluxvisuals$countMap.get(incoming);
       if (currentCount == null) {
          fluxvisuals$pendingStack = incoming;
          return false;
       }

       for (int i = messages.size() - 1; i >= 0; i--) {
          String s = fluxvisuals$getTextFromLine(messages.get(i));
          if (s != null && fluxvisuals$stripStack(s).equals(incoming)) {
             messages.remove(i);
             break;
          }
       }
       for (int i = visibleMessages.size() - 1; i >= 0; i--) {
          String vs = fluxvisuals$getTextFromVisibleLine(visibleMessages.get(i));
          if (vs != null && vs.trim().startsWith(incoming)) {
             visibleMessages.remove(i);
          }
       }

       int count = currentCount + 1;
       MutableText newText = message.copy().append(Text.literal(" \u00a77(x" + count + ")"));
       int ticks = mc.inGameHud.getTicks();
       ChatHudLine updated = new ChatHudLine(ticks, newText, null, MessageIndicator.system());
       messages.add(0, updated);
       ((BetterMinecraftChatAccessor) mc.inGameHud.getChatHud()).fluxvisuals$invokeAddVisibleMessage(updated);
       fluxvisuals$countMap.put(incoming, count);
       return true;
    }

    @Unique
    private String fluxvisuals$stripStack(String s) {
       Matcher m = fluxvisuals$STACK_PATTERN.matcher(s);
       return m.matches() ? m.group(1).trim() : s.trim();
    }

    @Unique
    private String fluxvisuals$getTextFromLine(ChatHudLine line) {
       try {
          RecordComponent[] comps = ChatHudLine.class.getRecordComponents();
          if (comps != null) {
             for (RecordComponent rc : comps) {
                if (Text.class.isAssignableFrom(rc.getType())) {
                   Object val = rc.getAccessor().invoke(line);
                   if (val instanceof Text t) return t.getString();
                }
             }
          }
       } catch (Exception ignored) {}
       try {
          for (java.lang.reflect.Method m : ChatHudLine.class.getDeclaredMethods()) {
             m.setAccessible(true);
             if (m.getParameterCount() == 0 && Text.class.isAssignableFrom(m.getReturnType())) {
                Object val = m.invoke(line);
                if (val instanceof Text t) return t.getString();
             }
          }
       } catch (Exception ignored) {}
       return null;
    }

    @Unique
    private String fluxvisuals$getTextFromVisibleLine(ChatHudLine.Visible line) {
       try {
          for (java.lang.reflect.Field f : ChatHudLine.Visible.class.getDeclaredFields()) {
             f.setAccessible(true);
             Object val = f.get(line);
             if (val instanceof OrderedText ot) {
                StringBuilder sb = new StringBuilder();
                ot.accept((i, style, cp) -> { sb.append(Character.toChars(cp)); return true; });
                return sb.toString();
             }
             if (val instanceof Text t) return t.getString();
          }
       } catch (Exception ignored) {}
       return null;
    }
}
