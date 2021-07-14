package com.ryanthetechman.death_detector.mixin;

import com.ryanthetechman.death_detector.util.ClientChatCallback;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class OnChat {
    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;IIZ)V")
    private void addMessage(Text message, int messageId, int timestamp, boolean refresh, CallbackInfo info){
        ClientChatCallback.EVENT.invoker().dispatch(message);
    }
}
