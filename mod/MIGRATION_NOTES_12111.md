# FluxVisuals ClickGui 1.21.11 API migration notes

Mining sources: unzip from
`D:/FluxVisuals/mod/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-05e7b299ed/1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.6/minecraft-merged-05e7b299ed-1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.6-sources.jar`

Build: `cd D:\FluxVisuals\mod && ./gradlew compileJava` — do NOT run two compiles at once (gradle lock).

## Entity position
- `Entity.getPos()` NO LONGER EXISTS in 1.21.11. Replace `e.getPos()` → `new Vec3d(e.getX(), e.getY(), e.getZ())`.
  - `camera.getPos()` → `camera.getCameraPos()`.
  - `a.getPos().subtract(b)` → `new Vec3d(a.getX(), a.getY(), a.getZ()).subtract(...)`.
- `prevX`/`prevZ`/`prevYaw`/`prevPitch` REMOVED. Replace `e.prevX` → `e.lastRenderX`, `e.prevZ` → `e.lastRenderZ`. For yaw/pitch prev: `PlayerEntity` still has `prevYaw`? NO — removed. Use `lastRenderYaw`/`lastRenderPitch` if present, else drop interpolation.
- `Entity.getYaw()` / `getPitch()` EXIST (float). `getYaw(float)/getPitch(float)` also exist (partial tick).

## Armor / equipment
- `LivingEntity.getArmorItems()` REMOVED. Use per-slot: `e.getEquippedStack(EquipmentSlot.HEAD/CHEST/LEGS/FEET)`.
- `PlayerInventory.armor` field REMOVED (PlayerInventory is `net.minecraft.client.item.PlayerInventory`). Use `getEquippedStack(EquipmentSlot.X)`.
  - Old `armor` indexes: [0]=FEET [1]=LEGS [2]=CHEST [3]=HEAD.

## Projectiles
- `PersistentProjectileEntity.getDamage()` REMOVED. Field `damage` is private but accesswidener exposes it: `net/minecraft/entity/projectile/PersistentProjectileEntity damage D` (already added). Use `p.damage`.
- `TridentEntity.getOwner()` still exists via ProjectileEntity.getOwner() (returns Entity). `persistentProjectileEntity.getOwner()` works.

## Velocity packet
- `EntityVelocityUpdateS2CPacket` has NO getVelocityX/Y/Z. Has `getVelocity()` returning `Vec3d`. Use `.x/.y/.z`. Values are already in blocks/tick (do not divide by 8000).

## Chat / message packets
- `GameMessageS2CPacket.content()` → `comp_763()` (returns Text).
- `CommandExecutionS2CPacket` (server-side command): named field differs — inspect actual packet source. For `PvPSafeModule.packet.command()` check the packet class actually imported.

## Text / Clickable
- `ClickEvent.suggestCommand(String)` REMOVED → `new ClickEvent.SuggestCommand(String)`.
- `ClickEvent.runCommand(String)` REMOVED → `new ClickEvent.RunCommand(String)`.
- `HoverEvent.showText(Content)` static factory REMOVED → `new HoverEvent.ShowText(Text)`.
- `GameProfile.getName()` REMOVED → `.name()` (record accessor).
- `Style.withClickEvent(ClickEvent)` unchanged.

## Client input / movement model (1.21.11 overhaul)
- `mc.player.input` is `net.minecraft.client.input.Input`. It has:
  - `Input.playerInput` (a `PlayerInput` record of booleans: forward, backward, left/strafe, right, sneak, jump).
  - `Input.jump()` method.
  - `Input.getMovementInput()` → `Vec2f` (forward, strafe).
  - NO `movementForward` / `movementSideways` float fields.
- Old `player.input.movementForward` → `player.input.getMovementInput().y` (forward). `movementSideways` → `player.input.getMovementInput().x`.
- Writing jump: `player.input.jump()` OR set `player.input.playerInput = new PlayerInput(..., jump=true, ...)`. Prefer `player.input.jump()`.
- `PlayerInput` record ctor: `PlayerInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak, boolean ???)`  — 7 booleans. See `net/minecraft/util/PlayerInput.java`.

## Rotation
- Server-side rotation packets / `ClientPlayerEntity` yaw: `getYaw()/getPitch()` exist.
- Interpolated yaw/pitch previously from `prevYaw/prevPitch` (removed). Use current `getYaw()/getPitch()` or `getYaw(delta)`.

## Misc
- `MinecraftClient.getTickDelta(boolean)` REMOVED → `getTickDelta()`.
- `Framebuffer` (net.minecraft.client.gl.Framebuffer) has NO beginWrite/endWrite — GPU backend. RenderTarget/offscreen capture must use `drawBlit(GpuTextureView)` / render direct. Architectural.
- HTTP: `errorBody()` → `error().body()`.
- `Enchantment` is a record; `Enchantments.FEATHER_FALLING` is a `RegistryKey`, use `registry.getOptionalValue(key)` then `registry.getEntry(value)`.
- Food: `itemStack.getItem().getFoodComponent()` — food `nutrition()`/`saturationModifier()` may be `.nutrition()`/`.saturationModifier()` on FoodComponent; check FoodComponent record accessors.
- `ButtonWidget.of(MutableText)` / overloads — check `ButtonWidget` factory signatures for 1.21.11.
- `Scoreboard`: `player.getScoreboard()` REMOVED → `player.getEntityWorld().getScoreboard()` or `player.world.getScoreboard()`.