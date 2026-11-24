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
-- Auto generated script file --

-- Holy Greatsword
function events.item_render(item)
    if fluxvisualsIsWeapon(item.id) then
        FLUXVISUALS_CUSTOM_WEAPON_ID = item.id
        return models.thegreatersword.Item
    end
end
