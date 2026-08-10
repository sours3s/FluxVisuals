package ru.fluxvisuals.ui.gui.widget.settings;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.impl.utils.*;
import ru.fluxvisuals.module.impl.visuals.*;
import ru.fluxvisuals.module.impl.visuals.HUD.*;

/**
 * Регистратор описаний для тултипов ClickGUI.
 * Вызывается один раз при инициализации клиента.
 */
@Environment(EnvType.CLIENT)
public final class TooltipRegistrar {
   public static void registerAll() {
      // === Visuals ===
      registerModule(GlassHand.class, "Рендерит руку от первого лица с эффектом стекла/свечения/контура. Режим \"Призрак\" комбинирует все эффекты.");
      registerModule(ShaderHand.class, "Шейдерные эффекты для руки: волна, глитч, хроматическая аберрация, контур, пульсация.");
      registerModule(CameraCustomer.class, "Настройка камеры: зум (FOV), аспект, трепетание, сглаживание, смещение камеры.");
      registerModule(BlockOutline.class, "Подсвечивает целевой блок контуром, заливкой или обоим вместе. Работает не сквозь стены.");
      registerModule(GlowCubes.class, "Светящиеся кубы вокруг игрока с анимацией появления/исчезновения и эффектом свечения.");
      registerModule(HitEffect.class, "Эффект волны при попадании по сущности. Рисует расширяющееся кольцо в точке попадания.");
      registerModule(KillEffect.class, "Эффект при убийстве сущности: частицы \"Soul\" (души) или \"Shatter\" (осколки).");
      registerModule(JumpCircle.class, "Красивые кольца/эффекты при прыжке: Pulse, Spiral, Nova, Blocks (подсветка блоков).");
      registerModule(ESP.class, "Классический ESP для игроков: боксы, линии, скелет, здоровье, имя, дистанция, прицеливание.");
      registerModule(Particles.class, "Различные партикл-эффекты: тотем-частицы, критические удары, магия, дым и др.");
      registerModule(Gamma.class, "Яркость (гамма) — позволяет видеть в темноте без факелов.");
      registerModule(TargetESP.class, "ESP для цели в прицеле: бокс, линии, здоровье, название, дистанция.");
      registerModule(SkinManager.class, "Менеджер скинов: загрузка кастомных скинов, случайные скины, скины по никам.");
      registerModule(SmoothCamera.class, "Сглаживание вращения камеры для плавных движений головы (third-person).");
      registerModule(ItemPhysics.class, "Физика предметов на земле: вращение, парение, анимация подбора.");
      registerModule(SwingAnimation.class, "Анимация замаха руки: старый стиль, сглаживание, раскачивание, 1.7/1.8/1.9 стили.");
      registerModule(NoRender.class, "Отключает рендер выбранных элементов: туман, небо, облака, партиклы, эндерменов и др.");
      registerModule(BetterWorld.class, "Улучшенный мир: цвет неба/тумана, яркость, отсутствие погоды, кастомное время.");
      registerModule(CustomWorld.class, "Полная кастомизация мира: небо, туман, облака, солнце/луна, звезды, погода.");
      registerModule(ProjectilePrediction.class, "Предсказание траектории снарядов (стрелы, снежки, яйца, треш).");
      registerModule(ItemESP.class, "ESP для предметов на земле: название, раритет, дистанция, фильтр по ценности.");
      registerModule(NameTags.class, "Ники над игроками: здоровье, броня, дистанция, команда, кастомные префиксы.");
      registerModule(AspectRation.class, "Изменение соотношения сторон экрана (16:9, 4:3, 21:9 и кастомное).");
      registerModule(MenuSettingsModule.class, "Настройки главного меню: фон, анимации, кастомный текст, версия.");
      registerModule(Friends.class, "Система друзей: зелёный ESP (не сквозь стены), тэг [F], защита от френдли-файра.");
      registerModule(Cosmetic.class, "Косметика: китайская шляпа/нимб, анимированные крылья, след за игроком.");
      registerModule(InventoryHUD.class, "Мини-инвентарь на экране + счётчики тотемов и стрел с автообновлением.");
      registerModule(Freecam.class, "Отсоединяет камеру от игрока; движение ограничено — камера не проходит сквозь твёрдые блоки.");

      // === Utils ===
      registerModule(DiscordRCP.class, "Discord Rich Presence: показывает сервер, модуль, активность в профиле.");
      registerModule(HitSound.class, "Звук при попадании: классический, металл, бабл, кастомный файл.");
      registerModule(SoundFX.class, "Кастомные звуки: отрыв тотема, лом брони, лом инструмента с громкостью.");
      registerModule(ClientSound.class, "Звук переключения модулей (вкл/выкл) с регулируемой громкостью.");
      registerModule(LagDetector.class, "Детектор лагов сервера: предупреждение о низком TPS и скачках пинга.");
      registerModule(ChatSounds.class, "Звуки для чата: сообщения, приватные, упоминания, команды.");

      // === Settings descriptions ===
      // GlassHand
      registerSetting(GlassHand.class, "Mode", "Режим отображения: Стекло (прозрачный куб), Свечение (билиборды), Контур (линии), Призрак (все вместе).");
      registerSetting(GlassHand.class, "Alpha", "Базовая непрозрачность эффекта (0-255).");
      registerSetting(GlassHand.class, "Intensity", "Интенсивность эффекта (множитель альфы).");
      registerSetting(GlassHand.class, "Pulse Speed", "Скорость пульсации размера/альфы.");
      registerSetting(GlassHand.class, "Glow Radius", "Радиус глоу-эффекта (для режима Свечение/Призрак).");

      // ShaderHand
      registerSetting(ShaderHand.class, "Mode", "Тип шейдера: Wave (волна), Glitch (глитч), Chromatic (хром. аберрация), Outline (контур), Pulse (пульсация).");
      registerSetting(ShaderHand.class, "Enabled", "Включить шейдерный эффект.");
      registerSetting(ShaderHand.class, "Intensity", "Сила эффекта (1.0 = нормально).");
      registerSetting(ShaderHand.class, "Speed", "Скорость анимации.");
      registerSetting(ShaderHand.class, "Scale", "Масштаб эффекта.");

      // CameraCustomer
      registerSetting(CameraCustomer.class, "Zoom Mode", "HOLD — зум зажат, TOGGLE — переключение.");
      registerSetting(CameraCustomer.class, "Zoom Key", "Клавиша для зума.");
      registerSetting(CameraCustomer.class, "Zoom Amount", "Во сколько раз уменьшается FOV (зум).");
      registerSetting(CameraCustomer.class, "Zoom Scroll", "Скорость изменения зума колесом мыши.");
      registerSetting(CameraCustomer.class, "Zoom Smooth", "Плавная интерполяция зума.");
      registerSetting(CameraCustomer.class, "Zoom Smooth Speed", "Скорость сглаживания зума (мс).");
      registerSetting(CameraCustomer.class, "Zoom Cinematic", "Включает cinematic camera (сглаженное движение).");
      registerSetting(CameraCustomer.class, "Zoom Hands", "Прятать руки при зуме.");
      registerSetting(CameraCustomer.class, "FOV Enabled", "Включить кастомный FOV отдельно от зума.");
      registerSetting(CameraCustomer.class, "FOV Value", "Значение FOV в градусах (30-130).");
      registerSetting(CameraCustomer.class, "FOV Smooth", "Плавное изменение FOV.");
      registerSetting(CameraCustomer.class, "Camera Distance", "Отодвинуть камеру назад (third-person-like).");
      registerSetting(CameraCustomer.class, "Camera Shake", "Трепетание камеры при зуме.");
      registerSetting(CameraCustomer.class, "Shake Intensity", "Сила трепетания.");

      // BlockOutline
      registerSetting(BlockOutline.class, "Mode", "Overlay (заливка), Outline (контур), Both (оба вместе).");
      registerSetting(BlockOutline.class, "Fill", "Рисовать заливку внутри контура.");
      registerSetting(BlockOutline.class, "Line Width", "Толщина линий контура.");

      // GlowCubes
      registerSetting(GlowCubes.class, "Block Count", "Количество кубов вокруг игрока.");
      registerSetting(GlowCubes.class, "Cube Size", "Размер каждого куба.");

      // HitEffect
      registerSetting(HitEffect.class, "Duration", "Длительность эффекта в мс.");
      registerSetting(HitEffect.class, "Max Radius", "Максимальный радиус волны.");
      registerSetting(HitEffect.class, "Segments", "Количество сегментов круга (качество).");

      // KillEffect
      registerSetting(KillEffect.class, "Mobs", "Показывать эффект для мобов, а не только игроков.");
      registerSetting(KillEffect.class, "Effect Type", "Soul (души) или Shatter (осколки).");

      // JumpCircle
      registerSetting(JumpCircle.class, "Mode", "Pulse (пульсация), Spiral (спираль), Nova (вспышка), Blocks (подсветка блоков).");
      registerSetting(JumpCircle.class, "Radius", "Радиус кольца (Pulse/Spiral/Nova).");
      registerSetting(JumpCircle.class, "Block Radius", "Радиус подсветки блоков (Blocks).");
      registerSetting(JumpCircle.class, "Time", "Длительность анимации в мс.");
      registerSetting(JumpCircle.class, "Anim", "Скорость анимации спирали.");
      registerSetting(JumpCircle.class, "Animations", "Size (анимация размера), Alpha (анимация прозрачности).");

      // ESP
      registerSetting(ESP.class, "Box", "Тип бокса: Off, 2D, 3D, Corners.");
      registerSetting(ESP.class, "Line", "Линии к игроку: Off, Bottom, Crosshair, Top.");
      registerSetting(ESP.class, "Skeleton", "Скелет игрока (кости).");
      registerSetting(ESP.class, "Health", "Полоска здоровья.");
      registerSetting(ESP.class, "Name", "Никнейм игрока.");
      registerSetting(ESP.class, "Distance", "Дистанция до игрока.");
      registerSetting(ESP.class, "Target Line", "Линия к цели в прицеле.");
      registerSetting(ESP.class, "Only Target", "Показывать только для цели в прицеле.");
      registerSetting(ESP.class, "Max Distance", "Максимальная дистанция рендера.");

      // Friends
      registerSetting(Friends.class, "Friend ESP", "Включить зелёный ESP для друзей (не сквозь стены).");
      registerSetting(Friends.class, "Show [F] Tag", "Показывать тэг [F] перед ником друга.");
      registerSetting(Friends.class, "Friendly Fire Protection", "Запретить атаковать друзей (клик блокируется).");

      // Cosmetic
      registerSetting(Cosmetic.class, "Features", "China Hat (шляпа/нимб), Wings (крылья), Trail (след).");
      registerSetting(Cosmetic.class, "Hat Mode", "Шляпа (конус) или Нимб (круглое свечение).");
      registerSetting(Cosmetic.class, "Hat Self/Friends/Others", "Кому показывать шляпу.");
      registerSetting(Cosmetic.class, "Wing Size", "Размер крыльев.");
      registerSetting(Cosmetic.class, "Wing Speed", "Скорость взмаха крыльев.");
      registerSetting(Cosmetic.class, "Trail Self/Friends", "Кому показывать след.");
      registerSetting(Cosmetic.class, "Trail Opacity", "Прозрачность следа.");

      // InventoryHUD
      registerSetting(InventoryHUD.class, "Render Inventory", "Показывать мини-инвентарь на экране.");
      registerSetting(InventoryHUD.class, "Render Totems", "Показывать счётчик тотемов.");
      registerSetting(InventoryHUD.class, "Render Arrows", "Показывать счётчик стрел.");
      registerSetting(InventoryHUD.class, "Compact Mode", "Компактный режим (меньше места).");

      // Freecam
      registerSetting(Freecam.class, "Speed", "Скорость полёта камеры.");
      registerSetting(Freecam.class, "Smooth", "Сглаживание движения камеры.");
      registerSetting(Freecam.class, "Render Player", "Рендерить игрока в фрикаме.");

      // Hud (HUD elements settings)
      registerSetting(Hud.class, "Style", "Dark (тёмный), Glass (стекло с блуром).");
      registerSetting(Hud.class, "Show Items", "Показывать предметы цели сверху.");
      registerSetting(Hud.class, "Show On Hover", "Показывать только при наведении в прицел.");
      registerSetting(Hud.class, "Particles", "Показывать партиклы вокруг головы цели.");
      registerSetting(Hud.class, "Scale", "Масштаб всего HUD (0.5-2.0).");

      // DiscordRCP
      registerSetting(DiscordRCP.class, "Show Server", "Показывать название сервера.");
      registerSetting(DiscordRCP.class, "Show Module", "Показывать активный модуль.");
      registerSetting(DiscordRCP.class, "Show Players", "Показывать онлайн игроков.");

      // HitSound
      registerSetting(HitSound.class, "Sound", "Тип звука: Classic, Metal, Bubble, Custom.");
      registerSetting(HitSound.class, "Volume", "Громкость звука (0-100%).");

      // SoundFX
      registerSetting(SoundFX.class, "Totem Pop", "Звук при срабатывании тотема бессмертия.");
      registerSetting(SoundFX.class, "Totem Volume", "Громкость звука тотема.");
      registerSetting(SoundFX.class, "Armor Break", "Звук при поломке брони.");
      registerSetting(SoundFX.class, "Armor Volume", "Громкость звука брони.");
      registerSetting(SoundFX.class, "Tool Break", "Звук при поломке инструмента.");
      registerSetting(SoundFX.class, "Tool Volume", "Громкость звука инструмента.");

      // ClientSound
      registerSetting(ClientSound.class, "Value", "Громкость звука переключения модулей (0-100%).");

      // LagDetector
      registerSetting(LagDetector.class, "TPS Warning", "Предупреждать о низком TPS сервера.");
      registerSetting(LagDetector.class, "TPS Threshold", "Порог TPS для предупреждения.");
      registerSetting(LagDetector.class, "Ping Spike Warning", "Предупреждать о скачках пинга.");
      registerSetting(LagDetector.class, "Ping Threshold (ms)", "Порог пинга в мс для предупреждения.");
      registerSetting(LagDetector.class, "Notify Cooldown (s)", "Кулдаун между уведомлениями в секундах.");

      // ChatSounds
      registerSetting(ChatSounds.class, "Messages", "Звук при получении сообщения в чате.");
      registerSetting(ChatSounds.class, "Private", "Звук при личном сообщении.");
      registerSetting(ChatSounds.class, "Mentions", "Звук при упоминании ника.");
      registerSetting(ChatSounds.class, "Commands", "Звук при выполнении команды.");
   }

   private static void registerModule(Class<? extends Module> clazz, String desc) {
      try {
         Module m = clazz.getDeclaredConstructor().newInstance();
         TooltipManager.registerModule(m.name, desc);
      } catch (Exception e) {
         System.err.println("[TooltipRegistrar] Failed to register " + clazz.getSimpleName() + ": " + e.getMessage());
      }
   }

   private static void registerSetting(Class<? extends Module> clazz, String settingName, String desc) {
      try {
         Module m = clazz.getDeclaredConstructor().newInstance();
         for (ru.fluxvisuals.module.api.setting.Setting s : m.getSettings()) {
            if (s.name.equals(settingName)) {
               TooltipManager.registerSetting(m.name, settingName, desc);
               return;
            }
         }
         System.err.println("[TooltipRegistrar] Setting not found: " + clazz.getSimpleName() + "::" + settingName);
      } catch (Exception e) {
         System.err.println("[TooltipRegistrar] Failed to register setting " + clazz.getSimpleName() + "::" + settingName + ": " + e.getMessage());
      }
   }
}