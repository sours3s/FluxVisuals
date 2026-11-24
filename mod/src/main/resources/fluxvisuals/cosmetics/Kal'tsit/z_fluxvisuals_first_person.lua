-- >>> fluxvisuals first person

-- Written by FluxVisuals. Hides the avatar body from its wearer's own
-- camera so the camera is never inside the model; held items stay visible.
local fluxvisualsBody = nil
local fluxvisualsHidden = nil
local fluxvisualsVanillaHidden = false
local fluxvisualsRightArmHidden = false
-- Set by the avatar's own events.item_render when it returns its weapon model.
FLUXVISUALS_CUSTOM_WEAPON_ID = nil
local function fluxvisualsIsItem(part)
    local name = part:getName():lower()
    local parent = part:getParentType() or ''
    if parent == 'Item' or parent == 'Trident' or parent == 'Arrow'
        or parent == 'LeftItemPivot' or parent == 'RightItemPivot'
        or parent == 'LeftArm' or parent == 'RightArm' then
        return true
    end
    -- Keep hands, arms, sleeves and weapon handles visible in first person.
    if name:match('hand') ~= nil or name == 'arm'
        or name == 'leftarm' or name == 'rightarm'
        or name == 'leftsleeve' or name == 'rightsleeve'
        or name:match('glove') ~= nil or name:match('finger') ~= nil
        or name:match('knuckle') ~= nil or name:match('palm') ~= nil
        or name:match('l_h') ~= nil or name:match('r_h') ~= nil then
        return true
    end
    return name:match('item') ~= nil or name:match('sword') ~= nil
        or name:match('scythe') ~= nil or name:match('weapon') ~= nil
        or name:match('trident') ~= nil or name:match('blade') ~= nil
        or name:match('spear') ~= nil or name:match('axe') ~= nil
        or name:match('hammer') ~= nil or name:match('bow') ~= nil
        or name:match('staff') ~= nil
end
local function fluxvisualsCollect(part, blocked, out)
    if not part then return end
    local isItem = fluxvisualsIsItem(part)
    if not blocked and not isItem then
        out[#out + 1] = {part = part, visible = part:getVisible()}
    end
    for _, child in pairs(part:getChildren() or {}) do
        fluxvisualsCollect(child, blocked or isItem, out)
    end
end
local function fluxvisualsFindBody()
    local out = {}
    for _, model in pairs(models:getChildren() or {}) do
        fluxvisualsCollect(model, false, out)
    end
    return out
end
local function fluxvisualsApplyHide()
    if not host:isHost() then return end
    local hide = renderer:isFirstPerson()
    if hide then
        -- Avatar scripts re-show parts every render frame, so always re-hide.
        if not fluxvisualsBody then fluxvisualsBody = fluxvisualsFindBody() end
        for _, entry in ipairs(fluxvisualsBody) do
            entry.part:setVisible(false)
        end
        fluxvisualsHidden = true
    elseif fluxvisualsHidden then
        for _, entry in ipairs(fluxvisualsBody or {}) do
            entry.part:setVisible(entry.visible)
        end
        fluxvisualsHidden = false
    end
end
local function fluxvisualsApplyVanillaHide()
    if not host:isHost() or not vanilla_model then return end
    if renderer:isFirstPerson() then
        -- Match vanilla: no torso, legs or head in first person. Arms and
        -- held items are separate parts and stay visible.
        -- Never hide vanilla_model.PLAYER — it contains both arms.
        vanilla_model.HEAD:setVisible(false)
        vanilla_model.BODY:setVisible(false)
        vanilla_model.HAT:setVisible(false)
        vanilla_model.JACKET:setVisible(false)
        vanilla_model.LEFT_LEG:setVisible(false)
        vanilla_model.RIGHT_LEG:setVisible(false)
        vanilla_model.LEFT_PANTS:setVisible(false)
        vanilla_model.RIGHT_PANTS:setVisible(false)
        vanilla_model.CAPE:setVisible(false)
        vanilla_model.ARMOR:setVisible(false)
        vanilla_model.ELYTRA:setVisible(false)
        fluxvisualsVanillaHidden = true
    elseif fluxvisualsVanillaHidden then
        vanilla_model.HEAD:setVisible(nil)
        vanilla_model.BODY:setVisible(nil)
        vanilla_model.HAT:setVisible(nil)
        vanilla_model.JACKET:setVisible(nil)
        vanilla_model.LEFT_LEG:setVisible(nil)
        vanilla_model.RIGHT_LEG:setVisible(nil)
        vanilla_model.LEFT_PANTS:setVisible(nil)
        vanilla_model.RIGHT_PANTS:setVisible(nil)
        vanilla_model.CAPE:setVisible(nil)
        vanilla_model.ARMOR:setVisible(nil)
        vanilla_model.ELYTRA:setVisible(nil)
        fluxvisualsVanillaHidden = false
    end
end
local function fluxvisualsRightHandHasCustomWeapon()
    if not host:isHost() or not player:isLoaded() then return false end
    if not renderer:isFirstPerson() then return false end
    if player:isLeftHanded() then return false end
    local renderedId = FLUXVISUALS_CUSTOM_WEAPON_ID
    if not renderedId then return false end
    local held = player:getHeldItem(true)
    return held ~= nil and held.id == renderedId
end
local function fluxvisualsApplyRightArmHide()
    if not host:isHost() or not vanilla_model then return end
    local hide = fluxvisualsRightHandHasCustomWeapon()
    for _, part in ipairs({vanilla_model.RIGHT_ARM, vanilla_model.RIGHT_SLEEVE}) do
        if part then part:setVisible(hide and false or nil) end
    end
    fluxvisualsRightArmHidden = hide
end
local function fluxvisualsNormalizeCamera()
    if not host:isHost() then return end
    if not renderer:isFirstPerson() then return end
    -- Clear every Figura camera override for the wearer. Avatar scripts commonly
    -- move the camera to a hat or head pivot for first person; that places the
    -- camera inside the body. nil restores vanilla camera behaviour.
    renderer:setEyeOffset(nil)
    renderer:setCameraPos(nil)
    renderer:setCameraPivot(nil)
    renderer:setOffsetCameraPivot(nil)
    renderer:setCameraRot(nil)
    renderer:setOffsetCameraRot(nil)
end
events.TICK:register(fluxvisualsApplyHide)
events.TICK:register(fluxvisualsApplyVanillaHide)
events.TICK:register(fluxvisualsApplyRightArmHide)
events.TICK:register(fluxvisualsNormalizeCamera)
events.RENDER:register(fluxvisualsApplyHide)
events.RENDER:register(fluxvisualsApplyVanillaHide)
events.RENDER:register(fluxvisualsApplyRightArmHide)
events.RENDER:register(fluxvisualsNormalizeCamera)
events.POST_RENDER:register(fluxvisualsApplyHide)
events.POST_RENDER:register(fluxvisualsApplyVanillaHide)
events.POST_RENDER:register(fluxvisualsApplyRightArmHide)
events.POST_RENDER:register(fluxvisualsNormalizeCamera)

-- <<< fluxvisuals first person
