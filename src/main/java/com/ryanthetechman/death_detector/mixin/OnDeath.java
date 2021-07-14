package com.ryanthetechman.death_detector.mixin;

import com.ryanthetechman.death_detector.util.EntityOnDeathCallback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class OnDeath {
    @Inject(at = @At("HEAD"), method = "onDeath")
    private void onDeath(DamageSource source, CallbackInfo info){
        EntityOnDeathCallback.EVENT.invoker().dispatch(source);
    }
}
