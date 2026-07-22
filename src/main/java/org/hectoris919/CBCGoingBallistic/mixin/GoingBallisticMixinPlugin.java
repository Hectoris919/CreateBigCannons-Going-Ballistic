package org.hectoris919.CBCGoingBallistic.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class GoingBallisticMixinPlugin implements IMixinConfigPlugin {
	private static final String SABLE_MIXIN = "org.hectoris919.CBCGoingBallistic.mixin.SableCompatMixin";

	@Override
	public void onLoad(String mixinPackage) { }

	@Override
	public String getRefMapperConfig() { return null; }

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (!SABLE_MIXIN.equals(mixinClassName)) return true;
		try {
			LoadingModList loadingModList = LoadingModList.get();
			return loadingModList != null && loadingModList.getModFileById("sable") != null;
		} catch (RuntimeException | LinkageError ex) {
			return false;
		}
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

	@Override
	public List<String> getMixins() { return null; }

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}