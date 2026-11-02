local l = (bl and 1) or -1

if I:isEnchanted(item) then 

    if (
        -- Sword effect
        I:isOf(item, Items:get("minecraft:wooden_sword")) or
        I:isOf(item, Items:get("minecraft:stone_sword")) or
        I:isOf(item, Items:get("minecraft:copper_sword")) or
        I:isOf(item, Items:get("minecraft:iron_sword")) or
        I:isOf(item, Items:get("minecraft:golden_sword")) or
        I:isOf(item, Items:get("minecraft:diamond_sword")) or
        I:isOf(item, Items:get("minecraft:netherite_sword")) or
        I:isOf(item, Items:get("minecraft:trident"))

    ) then
        particleManager:addParticle(particles, false, 0, 0.420, 0.0275, 0, 0, 0, 10*l, 0, 0, 0, 0, 0, 2.85,
        Texture:of("minecraft", "textures/particle/glow_sword.png"),"ITEM", hand, "SPAWN", "ADDITIVE", 0, 111)
    end

    if (
        -- Axe effect
        I:isOf(item, Items:get("minecraft:wooden_axe")) or
        I:isOf(item, Items:get("minecraft:stone_axe")) or
        I:isOf(item, Items:get("minecraft:copper_axe")) or
        I:isOf(item, Items:get("minecraft:iron_axe")) or
        I:isOf(item, Items:get("minecraft:golden_axe")) or
        I:isOf(item, Items:get("minecraft:diamond_axe")) or
        I:isOf(item, Items:get("minecraft:netherite_axe"))

    ) then
        particleManager:addParticle(particles, false, 0, 0.420, 0.0275, 0, 0, 0, 10*l, 0, 0, 0, 0, 0, 2.85,
        Texture:of("minecraft", "textures/particle/glow_axe.png"),"ITEM", hand, "SPAWN", "ADDITIVE", 0, 111)
    end

    if (
        -- Shovel effect
        I:isOf(item, Items:get("minecraft:wooden_shovel")) or
        I:isOf(item, Items:get("minecraft:stone_shovel")) or
        I:isOf(item, Items:get("minecraft:copper_shovel")) or
        I:isOf(item, Items:get("minecraft:iron_shovel")) or
        I:isOf(item, Items:get("minecraft:golden_shovel")) or
        I:isOf(item, Items:get("minecraft:diamond_shovel")) or
        I:isOf(item, Items:get("minecraft:netherite_shovel"))

    ) then
        particleManager:addParticle(particles, false, 0, 0.420, 0.0275, 0, 0, 0, 10*l, 0, 0, 0, 0, 0, 2.85,
        Texture:of("minecraft", "textures/particle/glow_shovel.png"),"ITEM", hand, "SPAWN", "ADDITIVE", 0, 111)
    end

    if (
        -- Hoe effect
        I:isOf(item, Items:get("minecraft:wooden_hoe")) or
        I:isOf(item, Items:get("minecraft:stone_hoe")) or
        I:isOf(item, Items:get("minecraft:copper_hoe")) or
        I:isOf(item, Items:get("minecraft:iron_hoe")) or
        I:isOf(item, Items:get("minecraft:golden_hoe")) or
        I:isOf(item, Items:get("minecraft:diamond_hoe")) or
        I:isOf(item, Items:get("minecraft:netherite_hoe"))

    ) then
        particleManager:addParticle(particles, false, 0, 0.420, 0.0275, 0, 0, 0, 10*l, 0, 0, 0, 0, 0, 2.85,
        Texture:of("minecraft", "textures/particle/glow_hoe.png"),"ITEM", hand, "SPAWN", "ADDITIVE", 0, 111)
    end

    if (
        -- Pickaxe effect
        I:isOf(item, Items:get("minecraft:wooden_pickaxe")) or
        I:isOf(item, Items:get("minecraft:stone_pickaxe")) or
        I:isOf(item, Items:get("minecraft:copper_pickaxe")) or
        I:isOf(item, Items:get("minecraft:iron_pickaxe")) or
        I:isOf(item, Items:get("minecraft:golden_pickaxe")) or
        I:isOf(item, Items:get("minecraft:diamond_pickaxe")) or
        I:isOf(item, Items:get("minecraft:netherite_pickaxe"))
        
    ) then
        particleManager:addParticle(particles, false, 0, 0.420, 0.0275, 0, 0, 0, 10*l, 0, 0, 0, 0, 0, 2.85,
        Texture:of("minecraft", "textures/particle/glow_pickaxe.png"),"ITEM", hand, "SPAWN", "ADDITIVE", 0, 111)
    end

    if (
        -- Misc effect
        I:isOf(item, Items:get("minecraft:fishing_rod")) or
        I:isOf(item, Items:get("minecraft:warped_fungus_on_a_stick")) or
        I:isOf(item, Items:get("minecraft:carrot_on_a_stick")) or
        I:isOf(item, Items:get("minecraft:mace"))
        
    ) then
        particleManager:addParticle(particles, false, 0, 0.420, 0.0275, 0, 0, 0, 10*l, 0, 0, 0, 0, 0, 2.85,
        Texture:of("minecraft", "textures/particle/glow_misc.png"),"ITEM", hand, "SPAWN", "ADDITIVE", 0, 111)
    end

    if (

        I:isOf(item, Items:get("minecraft:bow")) or
        I:isOf(item, Items:get("minecraft:crossbow"))
        
    ) then
        particleManager:addParticle(particles, false, 0, 0, 0, 0, 0, 0, 10*l, 0, 0, 0, 0, 0, 2.85,
        Texture:of("minecraft", "textures/particle/glow_misc.png"),"ITEM", hand, "SPAWN", "ADDITIVE", 0, 111)
    end

end

-- Totem glow

if (

        I:isOf(item, Items:get("minecraft:totem_of_undying"))
        
    ) then
        particleManager:addParticle(particles, false, 0.08*l, 0.175, 0, 0, 0, 0, 0*l, 0, 0, 0, 0, 0, 2.40,
        Texture:of("minecraft", "textures/particle/glow_totem.png"),"ITEM", hand, "SPAWN", "ADDITIVE", 0, 240)
    end

if (
	I:isOf(item, Items:get("minecraft:totem_of_undying"))
) then
	M:moveX(matrices, 0.12 * l)
    M:moveY(matrices, -0.0385)
	M:moveZ(matrices, -0.1)

    M:rotateX(matrices, 0)
	M:rotateY(matrices, -27 * l)
	M:rotateZ(matrices, 9 * l)

	M:scale(matrices, 1, 1, 1)
end

-- Sounds

if swingCountPrev ~= P:getSwingCount(player) and mainHand and not I:isEmpty(item) then

    if (
        I:isOf(item, Items:get("minecraft:wooden_sword"))
    ) then
        S:playSound("${soundWoodenSword}", ${generalVolume})
    end

    if (
        I:isOf(item, Items:get("minecraft:stone_sword"))
    ) then
        S:playSound("${soundStoneSword}", ${generalVolume})
    end

    if (
        I:isOf(item, Items:get("minecraft:copper_sword"))
    ) then
        S:playSound("${soundCopperSword}", ${generalVolume})
    end

    if (
        I:isOf(item, Items:get("minecraft:iron_sword"))
    ) then
        S:playSound("${soundIronSword}", ${generalVolume})
    end

    if (
        I:isOf(item, Items:get("minecraft:golden_sword"))
    ) then
        S:playSound("${soundGoldenSword}", ${generalVolume})
    end

    if (
        I:isOf(item, Items:get("minecraft:diamond_sword"))
    ) then
        S:playSound("${soundDiamondSword}", ${generalVolume})
    end

    if (
        I:isOf(item, Items:get("minecraft:netherite_sword"))
    ) then
        S:playSound("${soundNetheriteSword}", ${generalVolume})
    end

    if (
        I:isOf(item, Items:get("minecraft:trident"))
    ) then
        S:playSound("${soundTrident}", ${generalVolume})
    end

    if (
        I:isOf(item, Items:get("minecraft:mace"))
    ) then
        S:playSound("${soundMace}", ${generalVolume})
    end

end
swingCountPrev = P:getSwingCount(player)
