/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.statuseffects;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.mc.event.PlayerInfoFooterChangedEvent;
import com.wynntils.models.statuseffects.event.StatusEffectsChangedEvent;
import com.wynntils.models.statuseffects.type.StatusEffect;
import com.wynntils.models.worlds.event.WorldStateEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class StatusEffectModel extends Model {
    /**
     * CG1 is the color and symbol used for the effect, and the strength modifier string (e.g. "79%")
     * NCG1 is for strength modifiers without a decimal, and the % sign
     * NCG2 is the decimal point and second \d+ option for strength modifiers with a decimal
     * CG2 is the actual name of the effect
     * CG3 is the duration string (eg. "1:23")
     * Note: Buffs like "+190 Main Attack Damage" will have the +190 be considered as part of the name.
     * Buffs like "17% Frenzy" will have the 17% be considered as part of the prefix.
     * This is because the 17% in Frenzy (and certain other buffs) can change, but the static scroll buffs cannot.
     * <p>
     * https://regexr.com/7999h
     *
     * <p>Originally taken from: <a href="https://github.com/Wynntils/Wynntils/pull/615">Legacy</a>
     */
    private static final Pattern STATUS_EFFECT_PATTERN =
            Pattern.compile("(.+?§7 ?(?:\\d+(?:\\.\\d+)?%)?) ?([%\\-+\\/\\da-zA-Z'\\s]+?) §[84a]\\((.+?)\\).*");

    private static final String STATUS_EFFECTS_TITLE = "§d§lStatus Effects";

    private List<StatusEffect> statusEffects = List.of();

    public StatusEffectModel() {
        super(List.of());
    }

    public List<StatusEffect> getStatusEffects() {
        return statusEffects;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldStateChanged(WorldStateEvent e) {
        statusEffects = List.of();
    }

    @SubscribeEvent
    public void onTabListCustomization(PlayerInfoFooterChangedEvent event) {
        String footer = event.getFooter();

        if (footer.isEmpty()) {
            if (!statusEffects.isEmpty()) {
                statusEffects = List.of(); // No timers, get rid of them
                WynntilsMod.postEvent(new StatusEffectsChangedEvent());
            }

            return;
        }

        if (!footer.startsWith(STATUS_EFFECTS_TITLE)) return;

        List<StatusEffect> newStatusEffects = new ArrayList<>();

        String[] effects = footer.split("\\s{2}"); // Effects are split up by 2 spaces
        for (String effect : effects) {
            String trimmedEffect = effect.trim();
            if (trimmedEffect.isEmpty()) continue;

            Matcher m = STATUS_EFFECT_PATTERN.matcher(trimmedEffect);
            if (!m.find()) continue;

            // See comment at STATUS_EFFECT_PATTERN definition for format description of these
            String prefix = m.group(1);
            String name = m.group(2);
            String displayedTime = m.group(3);
            newStatusEffects.add(new StatusEffect(name, displayedTime, prefix));
        }

        statusEffects = newStatusEffects;
        WynntilsMod.postEvent(new StatusEffectsChangedEvent());
    }
}