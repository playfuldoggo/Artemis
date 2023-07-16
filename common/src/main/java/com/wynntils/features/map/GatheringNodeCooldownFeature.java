/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.map;

import com.wynntils.core.config.Category;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.features.Feature;
import com.wynntils.models.profession.event.ProfessionNodeGatheredEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.MAP)
public class GatheringNodeCooldownFeature extends Feature {
    @SubscribeEvent
    public void onNodeGathered(ProfessionNodeGatheredEvent.LabelShown event) {
        event.setAddCooldownArmorstand(true);
    }
}