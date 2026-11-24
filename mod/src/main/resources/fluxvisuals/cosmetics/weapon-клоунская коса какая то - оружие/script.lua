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
			FLUXVISUALS_CUSTOM_WEAPON_ID = item.id
			return models.model.ItemDevilsknife:setScale(Scale)
		end
	end
end
