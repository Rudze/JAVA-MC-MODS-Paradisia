package com.rudze.bounties.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Restyles the lore of a bounty: a blank line under the item name, the "Objectif" and
 * "Récompense" headings in bold white rather than gold, and a dot in front of each line under them.
 */
@Mixin(targets = "io.ejekta.bountiful.components.BountyStack")
public abstract class BountyTooltipMixin {

    private static final String REQUIRED = "bountiful.tooltip.required";
    private static final String REWARDS = "bountiful.tooltip.rewards";
    private static final String DEBUG = "Bountiful Debug Info";

    @Inject(method = "genTooltip", at = @At("RETURN"), cancellable = true)
    private void nh_bounties$restyle(boolean isServer, TooltipFlag flag, CallbackInfoReturnable<List<MutableComponent>> cir) {
        List<MutableComponent> lines = cir.getReturnValue();
        if (lines.isEmpty()) {
            return;
        }
        List<MutableComponent> styled = new ArrayList<>(lines.size() + 1);
        styled.add(Component.empty());
        boolean inSection = false;
        for (MutableComponent line : lines) {
            String heading = nh_bounties$heading(line);
            if (heading != null) {
                styled.add(Component.translatable(heading).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
                inSection = true;
            } else if (line.getString().startsWith(DEBUG)) {
                // Advanced tooltips in pack mode end with debug lines, left as they are.
                inSection = false;
                styled.add(line);
            } else if (inSection) {
                styled.add(Component.literal(" • ").withStyle(ChatFormatting.GRAY).append(line));
            } else {
                styled.add(line);
            }
        }
        cir.setReturnValue(styled);
    }

    private static String nh_bounties$heading(Component line) {
        if (line.getContents() instanceof TranslatableContents contents
                && (REQUIRED.equals(contents.getKey()) || REWARDS.equals(contents.getKey()))) {
            return contents.getKey();
        }
        return null;
    }
}
