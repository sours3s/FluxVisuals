-- FluxVisuals: react to every held weapon type, not only swords
local function fluxvisualsIsWeapon(id)
    id = id or ""
    return id:find("sword") ~= nil or id:find("axe") ~= nil or id:find("bow") ~= nil
        or id:find("crossbow") ~= nil or id:find("trident") ~= nil or id:find("pickaxe") ~= nil
        or id:find("spear") ~= nil or id:find("dagger") ~= nil or id:find("hammer") ~= nil
        or id:find("claw") ~= nil or id:find("mace") ~= nil or id:find("staff") ~= nil
        or id:find("scythe") ~= nil
end
-- Auto generated script file --

-- Holy Greatsword
function events.item_render(item)
    if fluxvisualsIsWeapon(item.id) then
        return models.thegreatersword.Item
    end
end
