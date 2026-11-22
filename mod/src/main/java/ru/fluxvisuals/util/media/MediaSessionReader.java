package ru.fluxvisuals.util.media;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Потокобезопасный singleton для чтения текущей медиа-сессии Windows.
 *
 * <p>Чтение идёт через PowerShell и WinRT API
 * {@code GlobalSystemMediaTransportControlsSessionManager}. Никаких внешних
 * нативных библиотек не требуется: достаточно JDK и системного PowerShell.
 *
 * <p>Скрипт печатает пакеты вида:
 * <pre>
 * TITLE:...
 * ARTIST:...
 * POSITION:...
 * DURATION:...
 * PLAYING:True/False
 * SOURCE:...
 * THUMB:OK
 * ---END---
 * </pre>
 * либо {@code NONE} и {@code ---END---}, если активной сессии нет.
 */
@Environment(EnvType.CLIENT)
public final class MediaSessionReader {
   private static final MediaSessionReader INSTANCE = new MediaSessionReader();

   private static final Charset CP1251 = Charset.forName("windows-1251");
   private static final Set<String> ALLOWED_COMMANDS = Set.of(
      "TryTogglePlayPauseAsync", "TrySkipNextAsync", "TrySkipPreviousAsync"
   );

   private static final int NONE_GRACE_BATCHES = 4;
   private static final long HEARTBEAT_TIMEOUT_MS = 12000L;
   private static final long RESTART_DELAY_MS = 1200L;
   private static final long COMMAND_TIMEOUT_MS = 3000L;

   private volatile boolean running;
   private volatile boolean hasMedia;
   private volatile String title = "";
   private volatile String artist = "";
   private volatile String source = "";
   private volatile long positionMs;
   private volatile long durationMs;
   private volatile boolean playing;
   private volatile long pollTime;
   private volatile int noneCount;
   private volatile boolean thumbnailUpdated;
   private volatile boolean lastThumbOk;
   private volatile Path thumbnailPath;
   private volatile long lastHeartbeat;
   private volatile Process process;

   private Path tempDir;
   private Path scriptFile;
   private Path thumbFile;
   private Thread readerThread;
   private Thread watchdogThread;

   // Сглаживание позиции между пакетами: опрос идёт примерно раз в 700 мс,
   // поэтому без экстраполяции прогресс-бар двигался бы рывками.
   private long stablePositionMs = -1L;
   private long stablePositionReadNanos;
   private String stablePositionTrack = "";

   private MediaSessionReader() {
   }

   public static MediaSessionReader getInstance() {
      return INSTANCE;
   }

   /** Идемпотентный запуск. Повторный вызов во время работы ничего не делает. */
   public synchronized void start() {
      if (running || !isWindows()) {
         return;
      }

      try {
         Path dir = Files.createTempDirectory("fluxvisuals-media-");
         Path script = dir.resolve("query.ps1");
         Path thumb = dir.resolve("thumb.png");
         Files.writeString(script, buildScript(), StandardCharsets.UTF_8);

         this.tempDir = dir;
         this.scriptFile = script;
         this.thumbFile = thumb;
         this.thumbnailPath = thumb;
      } catch (IOException e) {
         this.tempDir = null;
         this.scriptFile = null;
         this.thumbFile = null;
         this.thumbnailPath = null;
         return;
      }

      this.noneCount = 0;
      this.lastHeartbeat = System.currentTimeMillis();
      this.running = true;

      this.readerThread = new Thread(this::readerLoop, "MediaSessionReader");
      this.readerThread.setDaemon(true);
      this.readerThread.start();

      this.watchdogThread = new Thread(this::watchdogLoop, "MediaSessionWatchdog");
      this.watchdogThread.setDaemon(true);
      this.watchdogThread.start();
   }

   /** Останавливает все потоки, убивает процесс PowerShell и очищает временные файлы. */
   public synchronized void stop() {
      this.running = false;

      Process active = this.process;
      if (active != null) {
         active.destroyForcibly();
         this.process = null;
      }

      Thread reader = this.readerThread;
      Thread watchdog = this.watchdogThread;
      if (reader != null) {
         reader.interrupt();
      }
      if (watchdog != null) {
         watchdog.interrupt();
      }

      joinThread(reader);
      joinThread(watchdog);

      this.readerThread = null;
      this.watchdogThread = null;

      this.hasMedia = false;
      this.title = "";
      this.artist = "";
      this.source = "";
      this.positionMs = 0L;
      this.durationMs = 0L;
      this.playing = false;
      this.pollTime = 0L;
      this.noneCount = 0;
      this.thumbnailUpdated = false;
      this.lastThumbOk = false;
      this.thumbnailPath = null;
      this.stablePositionMs = -1L;
      this.stablePositionTrack = "";

      cleanupTempFiles();
   }

   private static void joinThread(Thread thread) {
      if (thread == null || thread == Thread.currentThread()) {
         return;
      }
      try {
         thread.join(1000L);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

   private void readerLoop() {
      while (running) {
         try {
            startProcess();
            readOutput();
         } catch (Exception ignored) {
            // Скрипт может временно не запуститься — повторим после паузы.
         }

         if (!running) {
            break;
         }

         Process active = this.process;
         if (active != null && active.isAlive()) {
            active.destroyForcibly();
         }
         this.process = null;

         if (!sleep(RESTART_DELAY_MS)) {
            break;
         }
      }
   }

   private void watchdogLoop() {
      while (running) {
         Process active = this.process;
         if (active != null && active.isAlive()
            && System.currentTimeMillis() - lastHeartbeat > HEARTBEAT_TIMEOUT_MS) {
            active.destroyForcibly();
         }
         if (!sleep(1000L)) {
            break;
         }
      }
   }

   private void startProcess() throws IOException {
      Path script = this.scriptFile;
      Path thumb = this.thumbFile;
      if (script == null || thumb == null) {
         return;
      }

      ProcessBuilder builder = new ProcessBuilder(
         powershellExecutable(),
         "-NoProfile",
         "-NonInteractive",
         "-ExecutionPolicy",
         "Bypass",
         "-WindowStyle",
         "Hidden",
         "-File",
         script.toAbsolutePath().toString(),
         thumb.toAbsolutePath().toString()
      );
      builder.redirectErrorStream(true);

      Process spawned = builder.start();
      if (!running) {
         spawned.destroyForcibly();
         return;
      }

      this.process = spawned;
      this.lastHeartbeat = System.currentTimeMillis();
   }

   private void readOutput() throws IOException {
      Process active = this.process;
      if (active == null) {
         return;
      }

      try (BufferedReader reader = new BufferedReader(
         new InputStreamReader(active.getInputStream(), StandardCharsets.UTF_8))) {

         Batch batch = new Batch();
         String line;
         while (running && (line = reader.readLine()) != null) {
            lastHeartbeat = System.currentTimeMillis();
            if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
               line = line.substring(1);
            }
            line = line.strip();
            if (line.isEmpty()) {
               continue;
            }

            if (line.equals("---END---")) {
               publish(batch);
               batch = new Batch();
            } else if (line.equals("NONE")) {
               batch.hasMedia = false;
            } else if (line.startsWith("TITLE:")) {
               batch.title = line.substring(6).strip();
               batch.hasMedia = true;
            } else if (line.startsWith("ARTIST:")) {
               batch.artist = line.substring(7).strip();
            } else if (line.startsWith("POSITION:")) {
               batch.position = parseLong(line.substring(9));
            } else if (line.startsWith("DURATION:")) {
               batch.duration = parseLong(line.substring(9));
            } else if (line.startsWith("PLAYING:")) {
               batch.playing = Boolean.parseBoolean(line.substring(8).strip());
            } else if (line.startsWith("SOURCE:")) {
               batch.source = line.substring(7).strip();
            } else if (line.equals("THUMB:OK")) {
               batch.thumbnail = true;
            }
            // Строки ERROR:... игнорируются: пакет NONE/---END--- уже сообщит
            // об отсутствии данных, а процесс будет перезапущен watchdog'ом.
         }
      }
   }

   private void publish(Batch batch) {
      if (!batch.hasMedia) {
         int misses = ++noneCount;
         if (misses >= NONE_GRACE_BATCHES) {
            hasMedia = false;
            title = "";
            artist = "";
            source = "";
            positionMs = 0L;
            durationMs = 0L;
            playing = false;
            pollTime = 0L;
            stablePositionMs = -1L;
            stablePositionTrack = "";
            lastThumbOk = false;
         }
         return;
      }

      noneCount = 0;
      String newTitle = cleanMetadata(batch.title);
      String newArtist = cleanMetadata(batch.artist);
      boolean trackChanged = !newTitle.equals(title) || !newArtist.equals(artist);

      title = newTitle;
      artist = newArtist;
      source = cleanMetadata(batch.source);
      positionMs = Math.max(0L, batch.position);
      durationMs = Math.max(0L, batch.duration);
      playing = batch.playing;
      pollTime = System.currentTimeMillis();

      if (trackChanged) {
         stablePositionMs = -1L;
         stablePositionTrack = "";
         // Сигнал выставляется и без обложки: HUD в этом случае просто уберёт старую.
         if (batch.thumbnail || lastThumbOk) {
            thumbnailUpdated = true;
         }
      } else if (batch.thumbnail && !lastThumbOk) {
         // Обложка появилась позже, чем сам трек.
         thumbnailUpdated = true;
      }
      lastThumbOk = batch.thumbnail;
   }

   private static long parseLong(String value) {
      try {
         return Long.parseLong(value.strip());
      } catch (NumberFormatException e) {
         return 0L;
      }
   }

   /** Возвращает сглаженную позицию с учётом времени с момента последнего пакета. */
   public synchronized long getPositionMs() {
      long candidate = immediatePositionMs();
      String currentTrack = title + '\n' + artist;
      long now = System.nanoTime();

      if (!currentTrack.equals(stablePositionTrack) || stablePositionMs < 0L) {
         stablePositionTrack = currentTrack;
         stablePositionMs = candidate;
         stablePositionReadNanos = now;
         return stablePositionMs;
      }

      long elapsed = Math.max(0L, (now - stablePositionReadNanos) / 1_000_000L);
      long expected = stablePositionMs + (playing ? elapsed : 0L);
      long error = candidate - expected;
      long corrected;

      if (Math.abs(error) > 1800L) {
         corrected = candidate;
      } else {
         long correctionLimit = Math.max(0L, Math.round(elapsed * 0.3F));
         long wantedCorrection = Math.round(error * 0.08F);
         long correction = Math.max(-correctionLimit, Math.min(correctionLimit, wantedCorrection));
         corrected = expected + correction;
      }

      stablePositionMs = Math.max(0L, durationMs > 0L ? Math.min(corrected, durationMs) : corrected);
      stablePositionReadNanos = now;
      return stablePositionMs;
   }

   /** Позиция без сглаживания, полезна для отладки. */
   public synchronized long getImmediatePositionMs() {
      return immediatePositionMs();
   }

   private long immediatePositionMs() {
      long candidate = positionMs;
      if (playing && pollTime != 0L) {
         candidate += Math.max(0L, System.currentTimeMillis() - pollTime);
      }
      return Math.max(0L, durationMs > 0L ? Math.min(candidate, durationMs) : candidate);
   }

   public String getTitle() {
      return title;
   }

   public String getArtist() {
      return artist;
   }

   public long getDurationMs() {
      return durationMs;
   }

   public boolean isPlaying() {
      return playing;
   }

   public boolean hasMedia() {
      return hasMedia;
   }

   public boolean isRunning() {
      return running;
   }

   public String getSource() {
      return source;
   }

   /** Путь к файлу обложки внутри временной папки. */
   public Path getThumbnailPath() {
      return thumbnailPath;
   }

   /** Возвращает true один раз при смене трека; HUD сам решит, загрузить обложку или убрать. */
   public synchronized boolean consumeThumbnailUpdate() {
      boolean update = thumbnailUpdated;
      thumbnailUpdated = false;
      return update;
   }

   public void playPause() {
      sendCommand("TryTogglePlayPauseAsync");
   }

   public void next() {
      sendCommand("TrySkipNextAsync");
   }

   public void previous() {
      sendCommand("TrySkipPreviousAsync");
   }

   private void sendCommand(String method) {
      if (!running || !ALLOWED_COMMANDS.contains(method) || !isWindows()) {
         return;
      }

      Thread commandThread = new Thread(() -> runCommand(method), "MediaSessionCommand");
      commandThread.setDaemon(true);
      commandThread.start();
   }

   private void runCommand(String method) {
      Process command = null;
      try {
         ProcessBuilder builder = new ProcessBuilder(
            powershellExecutable(), "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
            "-ExecutionPolicy", "Bypass", "-Command", buildCommandScript(method)
         );
         builder.redirectErrorStream(true);
         command = builder.start();
         command.waitFor(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      } catch (Exception ignored) {
         // Управление медиа — best effort, ошибка не должна ломать рендер.
      } finally {
         if (command != null && command.isAlive()) {
            command.destroyForcibly();
         }
      }
   }

   private void cleanupTempFiles() {
      if (thumbFile != null) {
         try {
            Files.deleteIfExists(thumbFile);
            Files.deleteIfExists(Path.of(thumbFile.toString() + ".tmp"));
         } catch (IOException ignored) {
         }
      }
      if (scriptFile != null) {
         try {
            Files.deleteIfExists(scriptFile);
         } catch (IOException ignored) {
         }
      }
      if (tempDir != null) {
         try {
            Files.deleteIfExists(tempDir);
         } catch (IOException ignored) {
         }
      }
      this.tempDir = null;
      this.scriptFile = null;
      this.thumbFile = null;
   }

   private static boolean sleep(long millis) {
      try {
         Thread.sleep(millis);
         return true;
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         return false;
      }
   }

   private static boolean isWindows() {
      return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
   }

   private static String powershellExecutable() {
      String root = System.getenv("SystemRoot");
      if (root == null || root.isBlank()) {
         root = "C:\\Windows";
      }
      Path executable = Path.of(root, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
      return Files.isRegularFile(executable) ? executable.toString() : "powershell.exe";
   }

   private static String cleanMetadata(String value) {
      String text = value == null ? "" : value.strip();
      if (text.isEmpty() || text.indexOf('\uFFFD') >= 0 || !looksMojibake(text)) {
         return text;
      }

      try {
         String repaired = new String(text.getBytes(CP1251), StandardCharsets.UTF_8);
         if (repaired.indexOf('\uFFFD') < 0 && repaired.indexOf('?') < 0) {
            return repaired;
         }
      } catch (Exception ignored) {
      }
      return text;
   }

   private static boolean looksMojibake(String text) {
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == '\u0420' || c == '\u0421' || c == '\u0440' || c == '\u0441') {
            return true;
         }
      }
      return false;
   }

   /**
    * Скрипт опрашивает WinRT раз в ~700 мс и печатает один пакет за итерацию.
    * Обложка пишется во временный файл с последующим atomic move, чтобы Java
    * никогда не читал недозаписанный PNG.
    */
   private static String buildScript() {
      return String.join("\r\n",
         "$ErrorActionPreference='SilentlyContinue'",
         "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8",
         "try{",
         "  Add-Type -AssemblyName System.Runtime.WindowsRuntime",
         "  $atm=([System.WindowsRuntimeSystemExtensions].GetMethods()|?{$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'}|Select-Object -First 1)",
         "  function WA($o,$t){if(-not $atm){return $null};$task=$atm.MakeGenericMethod($t).Invoke($null,@($o));if($task.Wait(3000)){return $task.Result};return $null}",
         "  [void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]",
         "  [void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties,Windows.Media.Control,ContentType=WindowsRuntime]",
         "  [void][Windows.Storage.Streams.IRandomAccessStreamWithContentType,Windows.Storage.Streams,ContentType=WindowsRuntime]",
         "  function Get-Mgr { return WA ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]) }",
         "  $mgr=Get-Mgr",
         "}catch{ $mgr=$null }",
         "$tp=$args[0]",
         "while($true){",
         "  try{",
         "    if(-not $mgr){ $mgr=Get-Mgr }",
         "    $sessions=$null",
         "    if($mgr){ $sessions=$mgr.GetSessions() }",
         "    $s=$null",
         "    if($sessions){",
         "      foreach($sess in $sessions){",
         "        $info=$sess.GetPlaybackInfo()",
         "        if($info -and $info.PlaybackStatus.ToString() -eq 'Playing'){ $s=$sess; break }",
         "      }",
         "    }",
         "    if(-not $s -and $mgr){ $s=$mgr.GetCurrentSession() }",
         "    if(-not $s -and $sessions -and $sessions.Count -gt 0){ $s=$sessions[0] }",
         "    if(-not $s){ $mgr=Get-Mgr }",
         "    if($s){",
         "      $p=WA ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])",
         "      $tl=$s.GetTimelineProperties()",
         "      $info=$s.GetPlaybackInfo()",
         "      $isPlaying=$false",
         "      if($info){ $isPlaying=($info.PlaybackStatus.ToString() -eq 'Playing') }",
         "      $pos=[long]0",
         "      if($tl){",
         "        if($tl.Position){ $pos=[long]$tl.Position.TotalMilliseconds }",
         "        if($isPlaying -and $tl.LastUpdatedTime){",
         "          $diff=([DateTimeOffset]::UtcNow - $tl.LastUpdatedTime).TotalMilliseconds",
         "          if($diff -gt 0){ $pos += [long]$diff }",
         "        }",
         "      }",
         "      $dur=[long]0",
         "      if($tl -and $tl.EndTime){ $dur=[long]$tl.EndTime.TotalMilliseconds }",
         "      $t=''",
         "      $a=''",
         "      $src=''",
         "      if($p){ $t=[string]$p.Title; $a=[string]$p.Artist }",
         "      $src=[string]$s.SourceAppUserModelId",
         "      $t=($t -replace '[\\r\\n]+',' ').Trim()",
         "      $a=($a -replace '[\\r\\n]+',' ').Trim()",
         "      $src=($src -replace '[\\r\\n]+',' ').Trim()",
         "      Write-Output \"TITLE:$t\"",
         "      Write-Output \"ARTIST:$a\"",
         "      Write-Output \"POSITION:$pos\"",
         "      Write-Output \"DURATION:$dur\"",
         "      Write-Output \"PLAYING:$isPlaying\"",
         "      Write-Output \"SOURCE:$src\"",
         "      if($tp -and $p -and $p.Thumbnail){",
         "        try{",
         "          $st=WA ($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])",
         "          if($st){",
         "            $asm=([System.IO.WindowsRuntimeStreamExtensions].GetMethods()|?{$_.Name -eq 'AsStream' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IRandomAccessStream'}|Select-Object -First 1)",
         "            $ns=$asm.Invoke($null,@($st))",
         "            $tmp=$tp+'.tmp'",
         "            $fs=[IO.File]::Create($tmp)",
         "            try{ $ns.CopyTo($fs) } finally { $fs.Dispose() }",
         "            $ns.Dispose()",
         "            if([IO.File]::Exists($tmp)){ [IO.File]::Delete($tp); [IO.File]::Move($tmp,$tp) }",
         "            Write-Output 'THUMB:OK'",
         "          }else{ Write-Output 'THUMB:NONE' }",
         "        }catch{ Write-Output 'THUMB:NONE' }",
         "      }else{ Write-Output 'THUMB:NONE' }",
         "    }else{",
         "      Write-Output 'NONE'",
         "    }",
         "  }catch{ Write-Output 'NONE' }",
         "  Write-Output '---END---'",
         "  [Console]::Out.Flush()",
         "  Start-Sleep -Milliseconds 700",
         "}"
      );
   }

   private static String buildCommandScript(String method) {
      return String.join(";",
         "$ErrorActionPreference='SilentlyContinue'",
         "Add-Type -AssemblyName System.Runtime.WindowsRuntime",
         "$atm=([System.WindowsRuntimeSystemExtensions].GetMethods()|?{$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'}|Select-Object -First 1)",
         "function WA($o,$t){if(-not $atm){return $null};$task=$atm.MakeGenericMethod($t).Invoke($null,@($o));if($task.Wait(3000)){return $task.Result};return $null}",
         "[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]",
         "$mgr=WA ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])",
         "$s=$null",
         "if($mgr){",
         "  $sessions=$mgr.GetSessions()",
         "  foreach($sess in $sessions){",
         "    $info=$sess.GetPlaybackInfo()",
         "    if($info -and $info.PlaybackStatus.ToString() -eq 'Playing'){ $s=$sess; break }",
         "  }",
         "  if(-not $s){ $s=$mgr.GetCurrentSession() }",
         "  if(-not $s -and $sessions -and $sessions.Count -gt 0){ $s=$sessions[0] }",
         "}",
         "if($s){ [void]$s." + method + "() }"
      );
   }

   private static final class Batch {
      String title = "";
      String artist = "";
      String source = "";
      long position;
      long duration;
      boolean playing;
      boolean hasMedia;
      boolean thumbnail;
   }
}
