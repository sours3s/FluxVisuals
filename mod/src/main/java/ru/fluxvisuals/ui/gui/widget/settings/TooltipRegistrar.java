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
      registerModule(HandEffects.class, "Эффекты руки: стекло, свечение, контуры (Frame/Outline) и анимации Wave/Glitch/Chromatic/Pulse.");
      registerModule(BlockOutline.class, "Подсвечивает целевой блок контуром, заливкой или обоим вместе. Работает не сквозь стены.");
      registerModule(GlowCubes.class, "Светящиеся кубы вокруг игрока с анимацией появления/исчезновения и эффектом свечения.");
      registerModule(HitEffect.class, "Эффект волны при попадании по сущности. Рисует расширяющееся кольцо в точке попадания.");
      registerModule(KillEffect.class, "Кастомные эффекты при убийстве: луч, душа, осколки, тьма, шипы и scan.");
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
      registerModule(CustomWorld.class, "Единые настройки мира: время суток (пресеты и кастомные тики), погода и FullBright.");
      registerSetting(CustomWorld.class, "Time Mode", "Пресеты времени или Custom для ручной настройки тиков.");
      registerSetting(CustomWorld.class, "Time Enabled", "Подменять время суток на клиенте.");
      registerSetting(CustomWorld.class, "Time", "Множитель тиков для режима Custom: 1–40 × 500.");
      registerSetting(CustomWorld.class, "FullBright", "Постоянное ночное зрение без факелов.");
      registerSetting(CustomWorld.class, "Weather", "Принудительно задавать погоду.");
      registerSetting(CustomWorld.class, "WeatherMode", "Clear, Rain или Storm.");
      registerModule(ProjectilePrediction.class, "Предсказание траектории снарядов (стрелы, снежки, яйца, треш).");
      registerModule(ItemESP.class, "ESP для предметов на земле: название, раритет, дистанция, фильтр по ценности.");
      registerModule(NameTags.class, "Ники над игроками: здоровье, броня, дистанция, команда, кастомные префиксы.");
      registerModule(AspectRation.class, "Изменение соотношения сторон экрана (16:9, 4:3, 21:9 и кастомное).");
      registerModule(CustomFov.class, "Ручной FOV: задаёт угол обзора и игнорирует множитель скорости, Speed 5 и зум.");
      registerSetting(CustomFov.class, "FOV", "Угол обзора в градусах (30-170). Приоритетнее ванильного FOV и эффекта скорости.");
      registerModule(MenuSettingsModule.class, "Настройки главного меню: фон, анимации, кастомный текст, версия.");
      registerModule(Friends.class, "Система друзей: зелёный ESP (не сквозь стены), тэг [F], защита от френдли-файра.");
      registerModule(Cosmetic.class, "Косметика: китайская шляпа/нимб, анимированные крылья, след за игроком.");

      // === Utils ===
      registerModule(DiscordRCP.class, "Discord Rich Presence: показывает сервер, модуль, активность в профиле.");
      registerModule(HitSound.class, "Звук при попадании: классический, металл, бабл, кастомный файл.");
      registerModule(SoundFX.class, "Кастомные звуки: отрыв тотема, лом брони, лом инструмента с громкостью.");
      registerModule(ClientSound.class, "Звук переключения модулей (вкл/выкл) с регулируемой громкостью.");
      registerModule(LagDetector.class, "Детектор лагов сервера: предупреждение о низком TPS и скачках пинга.");
      registerModule(ChatSounds.class, "Звуки для чата: сообщения, приватные, упоминания, команды.");

      // === Settings descriptions ===
      // HandEffects
      registerSetting(HandEffects.class, "Enabled", "Включить эффект руки.");
      registerSetting(HandEffects.class, "Mode", "Glass — стеклянный куб, Glow — свечение, Frame — контур куба, Ghost — все три вместе; Wave/Glitch/Chromatic/Outline/Pulse — анимированные эффекты.");
      registerSetting(HandEffects.class, "Intensity", "Общая сила цвета и альфы эффекта.");
      registerSetting(HandEffects.class, "Alpha", "Базовая непрозрачность для Glass/Glow/Frame/Ghost (0-255).");
      registerSetting(HandEffects.class, "Glow Radius", "Радиус свечения в режиме Glow/Ghost.");
      registerSetting(HandEffects.class, "Speed", "Скорость пульсации и анимации.");
      registerSetting(HandEffects.class, "Scale", "Размер анимированного эффекта.");

      // BlockOutline
      registerSetting(BlockOutline.class, "Outline", "Рисовать сглаженный контур целевого блока.");
      registerSetting(BlockOutline.class, "Line Width", "Толщина линий контура.");
      registerSetting(BlockOutline.class, "Fill", "Заливать грани блока.");
      registerSetting(BlockOutline.class, "Fill Type", "Normal — однотонная заливка, Shader — процедурный эффект.");
      registerSetting(BlockOutline.class, "Fill Alpha", "Прозрачность однотонной заливки.");
      registerSetting(BlockOutline.class, "Shader", "Эффект заливки: Nebula, Stars, Web или Plasma.");
      registerSetting(BlockOutline.class, "Shader Speed", "Скорость анимации эффекта.");
      registerSetting(BlockOutline.class, "Shader Alpha", "Прозрачность процедурной заливки.");
      registerSetting(BlockOutline.class, "Animation", "Дополнительная анимация: Pulse или Wave.");
      registerSetting(BlockOutline.class, "Client Color", "Использовать цвет клиента.");
      registerSetting(BlockOutline.class, "Custom Color", "Свой цвет, если Client Color выключен.");

      // GlowCubes
      registerSetting(GlowCubes.class, "Block Count", "Количество кубов вокруг игрока.");
      registerSetting(GlowCubes.class, "Cube Size", "Размер каждого куба.");

      // HitEffect
      registerSetting(HitEffect.class, "Duration", "Длительность эффекта в мс.");
      registerSetting(HitEffect.class, "Max Radius", "Максимальный радиус волны.");
      registerSetting(HitEffect.class, "Segments", "Количество сегментов круга (качество).");

      // KillEffect
      registerSetting(KillEffect.class, "Effect Type", "Тип эффекта: Beam, Sky Beam, Soul, Shatter, Darkness, Spike Abduction, Body Particles или Scan.");
      registerSetting(KillEffect.class, "Players", "Показывать эффект при убийстве игроков.");
      registerSetting(KillEffect.class, "Mobs", "Показывать эффект при убийстве мобов.");
      registerSetting(KillEffect.class, "Color Mode", "Цвет эффекта: тема клиента или свой.");
      registerSetting(KillEffect.class, "Color", "Свой цвет эффекта при Color Mode = Custom.");
      registerSetting(KillEffect.class, "Duration", "Длительность эффекта в секундах.");
      registerSetting(KillEffect.class, "Particles", "Количество частиц, осколков и сегментов эффекта.");
      registerSetting(KillEffect.class, "Radius", "Радиус луча, разлёта осколков и ударной волны.");
      registerSetting(KillEffect.class, "Speed", "Скорость анимации эффекта.");
      registerSetting(KillEffect.class, "Beam Height", "Высота вертикального луча.");
      registerSetting(KillEffect.class, "Beam Width", "Толщина вертикального луча.");
      registerSetting(KillEffect.class, "Shockwave", "Рисовать расширяющееся кольцо в точке гибели.");
      registerSetting(KillEffect.class, "Sound", "Воспроизводить звук выбранного эффекта.");
      registerSetting(KillEffect.class, "Totem", "Срабатывать также при использовании тотема бессмертия.");
      registerSetting(KillEffect.class, "Hide Body", "Скрывать модель убитой сущности во время эффекта Body Particles.");
      registerSetting(KillEffect.class, "Zoom", "Кратковременное приближение камеры в режиме Scan.");
      registerSetting(KillEffect.class, "Shake", "Кратковременная тряска камеры в режиме Scan.");
      registerSetting(KillEffect.class, "Desaturate", "Серый оверлей вместо обесцвечивания мира.");

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