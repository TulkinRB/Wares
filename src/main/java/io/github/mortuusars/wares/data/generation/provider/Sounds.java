package io.github.mortuusars.wares.data.generation.provider;

import io.github.mortuusars.wares.Wares;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.common.data.SoundDefinitionsProvider;

public class Sounds extends SoundDefinitionsProvider {
    public Sounds(DataGenerator generator, ExistingFileHelper helper) {
        super(generator, Wares.ID, helper);
    }

    @Override
    public void registerSounds() {
        add(Wares.SoundEvents.PAPER_TEAR.get(), definition()
                .subtitle("subtitle.wares.paper.tear")
                .with(multiple(2, Wares.ID + ":item/paper_tear", 1f, 1)));

        add(Wares.SoundEvents.PAPER_CRACKLE.get(), definition()
                .subtitle("subtitle.wares.paper.crackle")
                .with(multiple(3, "item/book/open_flip", 1f, 1f))
                .with(sound(Wares.resource("item/paper")).volume(0.3f).pitch(0.85f))
                .with(sound(Wares.resource("item/paper")).volume(0.3f).pitch(1.25f)));

        add(Wares.SoundEvents.CARDBOARD_BREAK.get(), definition()
                .subtitle("subtitles.block.generic.break")
                .with(multiple(4, "block/scaffold/place", 0.6f, 0.6f)));
        add(Wares.SoundEvents.CARDBOARD_PLACE.get(), definition()
                .subtitle("subtitles.block.generic.place")
                .with(multiple(4, "block/scaffold/place", 0.7f, 0.8f)));
        add(Wares.SoundEvents.CARDBOARD_STEP.get(), definition()
                .subtitle("subtitles.block.generic.footsteps")
                .with(multiple(7, "step/scaffold", 0.6f, 0.7f)));
        add(Wares.SoundEvents.CARDBOARD_HIT.get(), definition()
                .subtitle("subtitles.block.generic.hit")
                .with(multiple(7, "step/scaffold", 0.5f, 0.7f)));
        add(Wares.SoundEvents.CARDBOARD_FALL.get(), definition()
                .with(multiple(7, "step/scaffold", 0.65f, 0.65f)));

        add(Wares.SoundEvents.WRITING.get(), definition()
                .subtitle("subtitle.wares.writing")
                .with(multiple(3, Wares.ID + ":block/delivery_table/writing", 0.9f, 1)));
        add(Wares.SoundEvents.DELIVERY_TABLE_OPEN.get(), definition()
                .subtitle("subtitle.wares.delivery_table.open")
                .with(sound("block/barrel/open1").volume(0.5f).pitch(0.8f),
                      sound("block/barrel/open2").volume(0.5f).pitch(0.8f)));
        add(Wares.SoundEvents.DELIVERY_TABLE_CLOSE.get(), definition()
                .subtitle("subtitle.wares.delivery_table.close")
                .with(sound("block/barrel/close").volume(0.5f).pitch(0.8f)));
        add(Wares.SoundEvents.CARDBOARD_BOX_USE.get(), definition()
                .with(multiple(4, Wares.ID + ":block/cardboard/hit", 1, 1)));

        add(Wares.SoundEvents.VILLAGER_WORK_PACKAGER.get(), definition()
                .subtitle("subtitle.entity.villager.work_packager")
                .with(multiple(4, Wares.ID + ":block/cardboard/hit", 1, 1)));
    }

    private SoundDefinition.Sound[] multiple(int count, String name, float volume, float pitch) {
        SoundDefinition.Sound[] sounds = new SoundDefinition.Sound[count];
        for (int i = 0; i < count; i++) {
            sounds[i] = sound(name + (i + 1)).volume(volume).pitch(pitch);
        }
        return sounds;
    }
}
