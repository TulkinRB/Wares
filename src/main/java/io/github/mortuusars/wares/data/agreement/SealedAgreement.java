package io.github.mortuusars.wares.data.agreement;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.wares.Wares;
import io.github.mortuusars.wares.data.agreement.component.SteppedInt;
import io.github.mortuusars.wares.data.agreement.component.TextProvider;
import io.github.mortuusars.wares.data.serialization.ComponentCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@SuppressWarnings("UnusedReturnValue")
public record SealedAgreement(String id,
                              TextProvider buyerName,
                              TextProvider buyerAddress,
                              TextProvider title,
                              TextProvider message,
                              String seal,
                              Component sealTooltip,
                              Component backsideMessage,
                              Either<ResourceLocation, List<ItemStack>> requested,
                              Either<ResourceLocation, List<ItemStack>> payment,
                              Either<Integer, SteppedInt> ordered,
                              Either<Integer, SteppedInt> experience,
                              Either<Integer, SteppedInt> deliveryTime,
                              Either<Integer, SteppedInt> expiresInSeconds) {

    public static final Codec<SealedAgreement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("id", "").forGetter(SealedAgreement::id),
            TextProvider.CODEC.optionalFieldOf("buyerName", TextProvider.EMPTY).forGetter(SealedAgreement::buyerName),
            TextProvider.CODEC.optionalFieldOf("buyerAddress", TextProvider.EMPTY).forGetter(SealedAgreement::buyerAddress),
            TextProvider.CODEC.optionalFieldOf("title", TextProvider.EMPTY).forGetter(SealedAgreement::title),
            TextProvider.CODEC.optionalFieldOf("message", TextProvider.EMPTY).forGetter(SealedAgreement::message),
            Codec.STRING.optionalFieldOf("seal", "default").forGetter(SealedAgreement::seal),
            ComponentCodec.CODEC.optionalFieldOf("sealTooltip", TextComponent.EMPTY).forGetter(SealedAgreement::sealTooltip),
            ComponentCodec.CODEC.optionalFieldOf("backsideMessage", TextComponent.EMPTY).forGetter(SealedAgreement::backsideMessage),
            Codec.either(ResourceLocation.CODEC, Codec.list(ItemStack.CODEC)).fieldOf("requested").forGetter(SealedAgreement::requested),
            Codec.either(ResourceLocation.CODEC, Codec.list(ItemStack.CODEC)).fieldOf("payment").forGetter(SealedAgreement::payment),
            Codec.either(Codec.INT, SteppedInt.CODEC).optionalFieldOf("ordered", Either.left(0)).forGetter(SealedAgreement::ordered),
            Codec.either(Codec.INT, SteppedInt.CODEC).optionalFieldOf("experience", Either.left(0)).forGetter(SealedAgreement::experience),
            Codec.either(Codec.INT, SteppedInt.CODEC).optionalFieldOf("deliveryTime", Either.left(-1)).forGetter(SealedAgreement::deliveryTime),
            Codec.either(Codec.INT, SteppedInt.CODEC).optionalFieldOf("expiresInSeconds", Either.left(-1)).forGetter(SealedAgreement::expiresInSeconds)
        ).apply(instance, SealedAgreement::new));

    public static Optional<SealedAgreement> fromItemStack(ItemStack itemStack) {
        @Nullable CompoundTag compoundTag = itemStack.getTag();
        if (compoundTag == null)
            return Optional.empty();

        try {
            DataResult<Pair<SealedAgreement, Tag>> result = CODEC.decode(NbtOps.INSTANCE, compoundTag);
            SealedAgreement agreement = result.getOrThrow(false, Wares.LOGGER::error).getFirst();
            return Optional.of(agreement);
        } catch (Exception e) {
            Wares.LOGGER.error("Failed to decode SealedAgreement from item : '" + itemStack + "'.\n" + e);
            return Optional.empty();
        }
    }

    public boolean toItemStack(ItemStack stack) {
        try {
            CompoundTag tag = stack.getOrCreateTag();
            DataResult<Tag> result = CODEC.encodeStart(NbtOps.INSTANCE, this);
            Tag encodedTag = result.getOrThrow(false, Wares.LOGGER::error);
            tag.merge((CompoundTag) encodedTag);
            return true;
        } catch (Exception e) {
            Wares.LOGGER.error("Failed to encode SealedAgreement to item :\n" + e);
            return false;
        }
    }

    public Agreement realize(ServerLevel level) {
        Random random = level.getRandom();

        int quantity = ordered.map(integer -> integer, steppedInt -> steppedInt.sample(random));

        int expiresIn = expiresInSeconds.map(integer -> integer, steppedInt -> steppedInt.sample(random));
        long expireTime = expiresIn <= 0 ? -1 : level.getGameTime() + expiresIn * 20L;

        return Agreement.builder().id(id)
                .buyerName(buyerName.get(random))
                .buyerAddress(buyerAddress.get(random))
                .title(title.get(random))
                .message(message.get(random))
                .seal(seal)
                .requestedItems(getStacks(requested, level, Agreement.MAX_REQUESTED_STACKS))
                .paymentItems(getStacks(payment, level, Agreement.MAX_PAYMENT_STACKS))
                .ordered(quantity)
                .experience(experience.map(integer -> integer, steppedInt -> steppedInt.sample(random)))
                .deliveryTime(deliveryTime.map(integer -> integer, steppedInt -> steppedInt.sample(random)))
                .expireTime(expireTime)
                .build();
    }

    private List<ItemStack> getStacks(Either<ResourceLocation, List<ItemStack>> lootTableOrItems, ServerLevel level, int stackLimit) {
        List<ItemStack> items = lootTableOrItems.map(tableLocation -> unpackLootTable(tableLocation, level), list -> list);
        return compressAndLimitStacks(items, stackLimit);
    }

    private List<ItemStack> unpackLootTable(ResourceLocation lootTablePath, ServerLevel level) {
        LootTable lootTable = level.getServer().getLootTables().get(lootTablePath);
        LootContext.Builder lootContextBuilder = new LootContext.Builder(level);
        return lootTable.getRandomItems(lootContextBuilder.create(LootContextParamSets.EMPTY));
    }

    private List<ItemStack> compressAndLimitStacks(List<ItemStack> stacks, int stackLimit) {
        SimpleContainer container = new SimpleContainer(stackLimit);

        for (ItemStack item : stacks) {
            container.addItem(item);
        }

        return container.removeAllItems();
    }

    @SuppressWarnings("unused")
    public static class Builder {
        private String id = "";
        private TextProvider buyerName = TextProvider.EMPTY;
        private TextProvider buyerAddress = TextProvider.EMPTY;
        private TextProvider title = TextProvider.EMPTY;
        private TextProvider message = TextProvider.EMPTY;
        private String seal = "default";
        private Component sealTooltip = TextComponent.EMPTY;
        private Component backsideMessage = TextComponent.EMPTY;
        private Either<ResourceLocation, List<ItemStack>> requested = Either.right(Collections.emptyList());
        private Either<ResourceLocation, List<ItemStack>> payment = Either.right(Collections.emptyList());
        private Either<Integer, SteppedInt> ordered = Either.left(0);
        private Either<Integer, SteppedInt> experience = Either.left(0);
        private Either<Integer, SteppedInt> deliveryTime = Either.left(-1);
        private Either<Integer, SteppedInt> expiresInSecond = Either.left(-1);

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder buyerName(TextProvider buyerName) {
            this.buyerName = buyerName;
            return this;
        }

        public Builder buyerAddress(TextProvider buyerAddress) {
            this.buyerAddress = buyerAddress;
            return this;
        }

        public Builder title(TextProvider title) {
            this.title = title;
            return this;
        }

        public Builder message(TextProvider message) {
            this.message = message;
            return this;
        }

        public Builder seal(String seal) {
            this.seal = seal;
            return this;
        }

        public Builder sealTooltip(Component sealTooltip) {
            this.sealTooltip = sealTooltip;
            return this;
        }

        public Builder backsideMessage(Component backsideMessage) {
            this.backsideMessage = backsideMessage;
            return this;
        }

        public Builder requested(ResourceLocation lootTable) {
            this.requested = Either.left(lootTable);
            return this;
        }

        public Builder payment(ResourceLocation lootTable) {
            this.payment = Either.left(lootTable);
            return this;
        }

        public Builder requested(List<ItemStack> items) {
            this.requested = Either.right(items);
            return this;
        }

        public Builder payment(List<ItemStack> items) {
            this.payment = Either.right(items);
            return this;
        }

        public Builder ordered(int ordered) {
            this.ordered = Either.left(ordered);
            return this;
        }

        public Builder ordered(SteppedInt ordered) {
            this.ordered = Either.right(ordered);
            return this;
        }

        public Builder experience(int experience) {
            this.experience = Either.left(experience);
            return this;
        }

        public Builder experience(SteppedInt experience) {
            this.experience = Either.right(experience);
            return this;
        }

        public Builder deliveryTime(int deliveryTime) {
            this.deliveryTime = Either.left(deliveryTime);
            return this;
        }

        public Builder deliveryTime(SteppedInt deliveryTime) {
            this.deliveryTime = Either.right(deliveryTime);
            return this;
        }

        public Builder expiresInSecond(int expiresInSecond) {
            this.expiresInSecond = Either.left(expiresInSecond);
            return this;
        }

        public Builder expiresInSecond(SteppedInt expiresInSecond) {
            this.expiresInSecond = Either.right(expiresInSecond);
            return this;
        }

        public SealedAgreement build() {
            return new SealedAgreement(id, buyerName, buyerAddress, title, message, seal, sealTooltip, backsideMessage,
                    requested, payment, ordered, experience, deliveryTime, expiresInSecond);
        }
    }
}