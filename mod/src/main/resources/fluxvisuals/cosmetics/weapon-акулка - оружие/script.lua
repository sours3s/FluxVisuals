local FLUXVISUALS_EXTRA_IDS = {
    -- ["my_mod:my_custom_weapon"] = true,
}
local FLUXVISUALS_TOKENS = { "sword" }
local FLUXVISUALS_EXCLUDES = {}
local function fluxvisualsIsWeapon(id)
    id = tostring(id or ""):lower()
    if FLUXVISUALS_EXTRA_IDS[id] then return true end
    for _, token in ipairs(FLUXVISUALS_EXCLUDES) do
        if id:find(token, 1, true) then return false end
    end
    for _, token in ipairs(FLUXVISUALS_TOKENS) do
        if id:find(token, 1, true) then return true end
    end
    return false
end
-- Blahaj Sword Avatar Script for Figura


models.items.ItemBlahaj:setVisible(false)
function events.item_render(item)
    local isSword = fluxvisualsIsWeapon(item.id) ~= nil
    models.items.ItemBlahaj:setVisible(isSword)
    if isSword then
        FLUXVISUALS_CUSTOM_WEAPON_ID = item.id
        return models.items.ItemBlahaj
    end
end
