package org.hectoris919.CBCGoingBallistic.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Matches an item by registry id and, optionally, by a partial 1.21.1 ItemStack NBT predicate.
 *
 * <p>Predicate syntax is appended to the item id using SNBT, for example:</p>
 * <pre>
 * createbigcannons:autocannon_cartridge{components:{"createbigcannons:projectile":[{item:{id:"createbigcannons:ap_autocannon_round"}}]}}
 * </pre>
 *
 * <p>The SNBT is matched as a subset of {@link ItemStack#save(HolderLookup.Provider)}. Compound
 * predicates only need to include keys that must be present; list predicates only need to include
 * elements that must be found somewhere in the target list.</p>
 */
public record ItemStackSelector(ResourceLocation itemId, CompoundTag stackPredicate, String sourceText, int specificity) {
	public ItemStackSelector {
		Objects.requireNonNull(itemId, "itemId");
		sourceText = sourceText == null ? itemId.toString() : sourceText;
		specificity = Math.max(0, specificity);
	}

	public static ItemStackSelector parse(String raw) throws CommandSyntaxException {
		if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Empty item selector");
		String trimmed = raw.trim();
		int predicateStart = trimmed.indexOf('{');
		if (predicateStart < 0) {
			return new ItemStackSelector(ResourceLocation.parse(trimmed), null, trimmed, 0);
		}
		if (!trimmed.endsWith("}")) throw new IllegalArgumentException("Item selector predicate must end with '}'");
		String rawItemId = trimmed.substring(0, predicateStart).trim();
		String rawPredicate = trimmed.substring(predicateStart).trim();
		if (rawItemId.isBlank()) throw new IllegalArgumentException("Item selector is missing an item id before the predicate");
		CompoundTag predicate = TagParser.parseTag(rawPredicate);
		return new ItemStackSelector(ResourceLocation.parse(rawItemId), predicate, trimmed, countSpecificity(predicate));
	}

	public boolean isPlainItemId() {
		return this.stackPredicate == null || this.stackPredicate.isEmpty();
	}

	public boolean matches(ItemStack stack, HolderLookup.Provider registries) {
		if (stack == null || stack.isEmpty()) return false;
		ResourceLocation candidateItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!this.itemId.equals(candidateItemId)) return false;
		if (this.isPlainItemId()) return true;
		if (registries == null) return false;

		try {
			Tag saved = stack.save(registries);
			return saved instanceof CompoundTag savedCompound && containsSubset(savedCompound, this.stackPredicate);
		} catch (RuntimeException | LinkageError ex) {
			return false;
		}
	}

	@Override
	public @NotNull String toString() {
		return this.sourceText;
	}

	private static boolean containsSubset(Tag actual, Tag expected) {
		if (expected instanceof CompoundTag expectedCompound) {
			if (!(actual instanceof CompoundTag actualCompound)) return false;
			for (String key : expectedCompound.getAllKeys()) {
				if (!actualCompound.contains(key)) return false;
				Tag actualChild = actualCompound.get(key);
				Tag expectedChild = expectedCompound.get(key);
				if (actualChild == null || expectedChild == null || !containsSubset(actualChild, expectedChild)) return false;
			}
			return true;
		}

		if (expected instanceof ListTag expectedList) {
			if (!(actual instanceof ListTag actualList)) return false;
			if (expectedList.isEmpty()) return true;
			if (expectedList.size() > actualList.size()) return false;

			Set<Integer> matchedActualIndices = new HashSet<>();
			for (Tag expectedChild : expectedList) {
				boolean found = false;
				for (int actualIndex = 0; actualIndex < actualList.size(); ++actualIndex) {
					if (matchedActualIndices.contains(actualIndex)) continue;
					if (containsSubset(actualList.get(actualIndex), expectedChild)) {
						matchedActualIndices.add(actualIndex);
						found = true;
						break;
					}
				}
				if (!found) return false;
			}
			return true;
		}

		if (actual instanceof NumericTag actualNumber && expected instanceof NumericTag expectedNumber) {
			return Double.compare(actualNumber.getAsDouble(), expectedNumber.getAsDouble()) == 0;
		}

		return actual.equals(expected);
	}

	private static int countSpecificity(Tag tag) {
		if (tag instanceof CompoundTag compound) {
			int count = compound.getAllKeys().size();
			for (String key : compound.getAllKeys()) {
				Tag child = compound.get(key);
				if (child != null) count += countSpecificity(child);
			}
			return count;
		}
		if (tag instanceof ListTag list) {
			int count = list.size();
			for (Tag value : list) count += countSpecificity(value);
			return count;
		}
		return tag == null ? 0 : 1;
	}
}
