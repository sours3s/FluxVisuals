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

models.model.ItemDevilsknife.handleBottom_outline:setPrimaryRenderType("translucent_cull")
models.model.ItemDevilsknife.handle_outline:setPrimaryRenderType("translucent_cull")
models.model.ItemDevilsknife.bladeBall_outline:setPrimaryRenderType("translucent_cull")
models.model.ItemDevilsknife.backBall_outline:setPrimaryRenderType("translucent_cull")

--function events.item_render(item)
--    if fluxvisualsIsWeapon(item.id) then
--        return models.model.ItemDevilsknife
--    end
--end

function events.item_render(item, context)
	if player:isLoaded() then
		if fluxvisualsIsWeapon(item.id) then
			local Scale = 1
			if context:find("FIRST_PERSON") then
				Scale = 0.75
			end
			return models.model.ItemDevilsknife:setScale(Scale)
		end
	end
end
