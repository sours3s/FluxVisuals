# ======================================================================
#  ProGuard rules — МАКСИМАЛЬНАЯ обфускация FluxVisuals mod
#  Классы/методы/поля переименовываются в ключевые слова Java, всё
#  сваливается в корневой пакет, строки шифруются, имена конфликтуют.
#  Reflection-точки (EventManager, конфиг, tooltips) защищены keep.
# ======================================================================

# --- Оптимизация + обфускация ---
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!class/unboxing/enum
-overloadaggressively
-useuniqueclassmembernames
-allowaccessmodification
-mergeinterfacesaggressively

# Ресурсы/файлы и НЕ-переименовываемое
-ignorewarnings
-dontnote
-verbose

# Атрибуты, критичные для миксинов/аннотаций/дженериков
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions
# Источник в байткоде подменяем — декомпилятор не покажет реальные имена
-renamesourcefileattribute SourceFile

# --- Словари конфликтных имён (ключевые слова Java = невалидный Java-код) ---
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary class-obfuscation-dictionary.txt
-packageobfuscationdictionary package-obfuscation-dictionary.txt

# --- Всё переименованное сваливаем в корневой пакет ---
-repackageclasses ''
-flattenpackagehierarchy ''

# --- Entrypoint Fabric (fabric.mod.json) ---
-keep public class ru.fluxvisuals.client.FluxVisualsClient { *; }

# --- Миксины: имена классов жёстко завязаны на mixins.json ---
-keep class ru.fluxvisuals.mixin.** { *; }

# --- Лицензионная защита (LaunchGate) — трогать нельзя ---
-keep class ru.fluxvisuals.license.** { *; }

# --- События и EventManager: invoke() по типам параметров ---
-keep class ru.fluxvisuals.event.** { *; }

# --- API модулей: Module/Setting/Manager/Category/аннотации — GUI и конфиг ---
-keep class ru.fluxvisuals.module.api.** { *; }

# --- Обработчики событий вызываются рефлексией (@EventInit) ---
-keepclassmembers class * {
    @ru.fluxvisuals.event.EventInit *;
}

# --- Классы, создаваемые через reflection (TooltipRegistrar и т.п.) ---
-keepclassmembers class * extends ru.fluxvisuals.module.api.Module {
    public <init>();
}

# --- GUI-скрины и API, к которым обращается vanilla/fabric ---
-keep class ru.fluxvisuals.screen.** { *; }

# --- Рендер-API: Gson-модели (FontData, шрифты), ClientRenderer и т.п.
# Поля FontData читаются Gson по имени — переименование ломает загрузку шрифтов (NPE). ---
-keep class ru.fluxvisuals.api.** { *; }

# --- Crosshair: Gson-сериализация настроек прицела ---
-keep class ru.fluxvisuals.crosshair.** { *; }
