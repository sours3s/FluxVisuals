-- FluxVisuals: react to every held weapon type, not only swords
local function fluxvisualsIsWeapon(id)
    id = id or ""
    return id:find("sword") ~= nil or id:find("axe") ~= nil or id:find("bow") ~= nil
        or id:find("crossbow") ~= nil or id:find("trident") ~= nil or id:find("pickaxe") ~= nil
        or id:find("spear") ~= nil or id:find("dagger") ~= nil or id:find("hammer") ~= nil
        or id:find("claw") ~= nil or id:find("mace") ~= nil or id:find("staff") ~= nil
        or id:find("scythe") ~= nil
end

local SwingingPhysics = require("libs.swinging_physics")
local swingOnHead = SwingingPhysics.swingOnHead
local swingOnBody = SwingingPhysics.swingOnBody

swingOnBody(models.model.root.Body.SaberBelt, 0, {-20,20,-20,20,-20,20})

--hide vanilla model
vanilla_model.PLAYER:setVisible(false)
vanilla_model.ARMOR:setVisible(false)

models.model.root.Head:setPrimaryTexture("SKIN")
models.model.root.Body.BD:setPrimaryTexture("SKIN")
models.model.root.RightArm:setPrimaryTexture("SKIN")
models.model.root.LeftArm:setPrimaryTexture("SKIN")
models.model.root.RightLeg:setPrimaryTexture("SKIN")
models.model.root.LeftLeg:setPrimaryTexture("SKIN")

-----------------------------------------------------------------
function events.tick()
   if player:getVelocity().xz:length() > .01 then
   end
   if player:getVelocity().xz:length() == 0 and (player:getItem(1).id == "minecraft:air") then
   end
   if not (player:getItem(1).id == "minecraft:air") then
   end
   if player:getItem(1).id:find("sword") then
       models.model.root.Body.SaberBelt.sb.Saber2:setVisible(false)
    else
       models.model.root.Body.SaberBelt.sb.Saber2:setVisible(true)
   end
end
-----------------------------------------------------------------
function events.ITEM_RENDER(item)
    if fluxvisualsIsWeapon(item.id) then
       return models.model.ItemV
    end
end
-----------------------------------------------------------------