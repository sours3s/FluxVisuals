package ru.fluxvisuals.license;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;

/**
 * Проверка запуска мода через лоадер FluxVisuals.
 *
 * <p>Лоадер передаёт в игру одноразовый RSA-подписанный launch-тикет (JWT RS256):
 * <pre>
 *   -Dfluxvisuals.ticket=…
 *   -Dfluxvisuals.challenge=…
 *   -Dfluxvisuals.authserver=…
 * </pre>
 * Мод проверяет подпись встроенным публичным ключом и claims, затем «сжигает» тикет на сервере
 * (одноразовый, replay отклоняется). Без валидного тикета клиент не инициализируется.
 */
public final class LaunchGate {
   private static final String PUB_KEY_RESOURCE = "/fluxvisuals/license/flux-public.pem";
   private static final String ISSUER = "FluxVisualsAuth";
   private static final String AUDIENCE = "FluxVisualsMod";
   private static final long MAX_CLOCK_SKEW_SECONDS = 30;

   private static volatile boolean verificationStarted;
   private static volatile boolean verified;

   private LaunchGate() {}

   /** Проверяет тикет (локально: подпись + claims) и асинхронно сжигает его на сервере. */
   public static boolean verify() {
      if (verificationStarted) return verified;
      synchronized (LaunchGate.class) {
         if (verificationStarted) return verified;
         verificationStarted = true;
         verified = doVerify();
      }
      return verified;
   }

   private static boolean doVerify() {
      try {
         String ticket = System.getProperty("fluxvisuals.ticket");
         String challenge = System.getProperty("fluxvisuals.challenge");
         String authServer = System.getProperty("fluxvisuals.authserver");
         if (ticket == null || ticket.isBlank() || challenge == null || challenge.isBlank()) {
            return false; // запущено не через лоадер
         }

         String[] parts = ticket.split("\\.");
         if (parts.length != 3) return false;

         byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
         byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
         byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
         String signingInput = parts[0] + "." + parts[1];

         // --- подпись RS256 ---
         PublicKey key = loadPublicKey();
         Signature sig = Signature.getInstance("SHA256withRSA");
         sig.initVerify(key);
         sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
         if (!sig.verify(signature)) return false;

         JsonObject header = JsonParser.parseString(new String(headerBytes, StandardCharsets.UTF_8)).getAsJsonObject();
         if (!"RS256".equals(str(header, "alg"))) return false;

         JsonObject payload = JsonParser.parseString(new String(payloadBytes, StandardCharsets.UTF_8)).getAsJsonObject();
         if (!ISSUER.equals(str(payload, "iss"))) return false;
         if (!AUDIENCE.equals(str(payload, "aud"))) return false;
         if (!challenge.equals(str(payload, "challenge"))) return false;
         if (str(payload, "jti").isBlank()) return false;
         if (!payload.has("exp") || !payload.get("exp").isJsonPrimitive()) return false;
         long exp = payload.get("exp").getAsLong();
         if (exp < System.currentTimeMillis() / 1000L - MAX_CLOCK_SKEW_SECONDS) return false;

         // Тикет валиден — сжигаем на сервере (одноразовый). Сетевая ошибка запуск не блокирует.
         consumeAsync(ticket, challenge, authServer);
         return true;
      } catch (Exception ex) {
         return false;
      }
   }

   private static String str(JsonObject obj, String key) {
      return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : "";
   }

   private static PublicKey loadPublicKey() throws Exception {
      try (var in = LaunchGate.class.getResourceAsStream(PUB_KEY_RESOURCE)) {
         if (in == null) throw new IllegalStateException("Public key resource not found: " + PUB_KEY_RESOURCE);
         String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8)
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
         byte[] der = Base64.getDecoder().decode(pem);
         return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
      }
   }

   private static void consumeAsync(String ticket, String challenge, String authServer) {
      Thread t = new Thread(() -> {
         try {
            if (authServer == null || authServer.isBlank()) return;
            String body = "{\"ticket\":\"" + escape(ticket) + "\",\"challenge\":\"" + escape(challenge) + "\"}";
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest req = HttpRequest.newBuilder()
               .uri(URI.create(authServer + "/api/launch/consume"))
               .timeout(Duration.ofSeconds(15))
               .header("Content-Type", "application/json")
               .POST(HttpRequest.BodyPublishers.ofString(body))
               .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
               ru.fluxvisuals.client.FluxVisualsClient.LOGGER.warn(
                  "Launch ticket consume failed: {} {}", resp.statusCode(), resp.body());
            }
         } catch (Exception ignored) {
            // локальная проверка уже прошла — сетевую ошибку consume не считаем блокирующей
         }
      }, "flux-launch-consume");
      t.setDaemon(true);
      t.start();
   }

   private static String escape(String s) {
      return s.replace("\\", "\\\\").replace("\"", "\\\"");
   }
}
