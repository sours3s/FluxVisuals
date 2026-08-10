package ru.fluxvisuals.discord;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Клиент Discord Rich Presence по прямому IPC (named pipe).
 *
 * Полностью WRITE-ONLY: шлём handshake + SET_ACTIVITY и не читаем ответы.
 * Никаких читающих потоков — значит close() из главного потока никогда не
 * заблокируется (это и был фриз майнкрафта при выключении модуля).
 *
 * Заменяет старую нативную discord-rpc.dll: та не умела кнопки, а Java-структура
 * была под патченную версию — поля не совпадали и кнопка не доходила до Discord.
 */
public final class DiscordIpc {
   public static final String APP_ID = "1533500628494712952";
   public static final String DISCORD_SERVER_URL = "https://discord.gg/HScqNfYcca";

   private static final int OP_HANDSHAKE = 0;
   private static final int OP_FRAME = 1;

   private final AtomicBoolean connected = new AtomicBoolean(false);
   private final AtomicBoolean connecting = new AtomicBoolean(false);
   private final AtomicLong nonce = new AtomicLong(0);
   private volatile RandomAccessFile pipe;

   public boolean isConnected() {
      return connected.get();
   }

   /** Вызывается ТОЛЬКО из фонового потока (никогда не из рендер-потока). */
   public void connect() {
      if (connected.get() || !connecting.compareAndSet(false, true)) {
         return;
      }
      try {
         String os = System.getProperty("os.name", "").toLowerCase();
         if (!os.contains("win")) {
            return; // unix-сокеты Java без доп. библиотек не открывает
         }
         for (int i = 0; i < 10; i++) { // Discord создаёт discord-ipc-0..9
            try {
               RandomAccessFile f = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
               send(f, OP_HANDSHAKE, "{\"v\":1,\"client_id\":\"" + APP_ID + "\"}");
               pipe = f;
               connected.set(true);
               return;
            } catch (IOException e) {
               // канал не открыт (Discord не запущен) — пробуем следующий
            }
         }
      } finally {
         connecting.set(false);
      }
   }

   public void updatePresence(String state, String details, long startTimestamp) {
      RandomAccessFile f = pipe;
      if (f == null || !connected.get()) {
         return;
      }
      StringBuilder activity = new StringBuilder("{");
      boolean first = true;
      if (state != null) {
         activity.append("\"state\":").append(q(state));
         first = false;
      }
      if (details != null) {
         if (!first) activity.append(',');
         activity.append("\"details\":").append(q(details));
         first = false;
      }
      if (!first) activity.append(',');
      activity.append("\"timestamps\":{\"start\":").append(startTimestamp).append("},");
      activity.append("\"assets\":{\"large_image\":\"flux_logo\",\"large_text\":\"Flux Visuals\",\"small_image\":\"flux_icon\"},");
      activity.append("\"buttons\":[{\"label\":\"Discord Server\",\"url\":").append(q(DISCORD_SERVER_URL)).append("}]");
      activity.append('}');

      String payload = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid() + ",\"activity\":" + activity
         + "},\"nonce\":" + q("nv" + nonce.incrementAndGet()) + "}";
      try {
         send(f, OP_FRAME, payload);
      } catch (IOException e) {
         disconnect(); // Discord закрыл канал — переподключимся позже
      }
   }

   /** Безопасно из любого потока: ничего не читает, поэтому close() не блокирует. */
   public void shutdown() {
      connected.set(false);
      connecting.set(false);
      RandomAccessFile f = pipe;
      pipe = null;
      if (f != null) {
         try {
            f.close();
         } catch (IOException ignored) {
         }
      }
   }

   private void disconnect() {
      connected.set(false);
      RandomAccessFile f = pipe;
      pipe = null;
      if (f != null) {
         try {
            f.close();
         } catch (IOException ignored) {
         }
      }
   }

   private static void send(RandomAccessFile f, int opcode, String json) throws IOException {
      byte[] payload = json.getBytes(StandardCharsets.UTF_8);
      writeIntLE(f, opcode);
      writeIntLE(f, payload.length);
      f.write(payload);
   }

   private static void writeIntLE(RandomAccessFile f, int v) throws IOException {
      f.write(v & 0xFF);
      f.write((v >>> 8) & 0xFF);
      f.write((v >>> 16) & 0xFF);
      f.write((v >>> 24) & 0xFF);
   }

   private static String q(String s) {
      if (s == null) {
         return "null";
      }
      StringBuilder sb = new StringBuilder("\"");
      for (char c : s.toCharArray()) {
         switch (c) {
            case '"':
               sb.append("\\\"");
               break;
            case '\\':
               sb.append("\\\\");
               break;
            case '\n':
               sb.append("\\n");
               break;
            case '\r':
               sb.append("\\r");
               break;
            case '\t':
               sb.append("\\t");
               break;
            default:
               sb.append(c);
         }
      }
      return sb.append('"').toString();
   }
}
