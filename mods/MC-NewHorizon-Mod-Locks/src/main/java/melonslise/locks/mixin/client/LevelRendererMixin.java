package melonslise.locks.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import melonslise.locks.client.event.LocksClientForgeEvents;
import melonslise.locks.client.util.LocksClientUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
	// 1.21.1 dropped the PoseStack param from renderLevel: the camera's rotation/position is applied via
	// RenderSystem's own model-view state (see the this.renderEntity(..., posestack, ...) calls in the real
	// method, which pass a freshly-built IDENTITY PoseStack, not one carrying poseMatrix) - only translating by
	// world-position-minus-camera-position, same as here. poseMatrix/projMatrix are the raw view/projection
	// matrices (used verbatim for the frustum, see the real method's own identical
	// new Frustum(poseMatrix, projMatrix) call) and must NOT be folded into the render PoseStack, or the
	// camera rotation gets applied twice.
	@Inject(at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/LevelRenderer.checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 0), method = "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V")
	private void renderLevel(DeltaTracker deltaTracker, boolean renderOutline, Camera cam, GameRenderer gr, LightTexture lightTex, Matrix4f poseMatrix, Matrix4f projMatrix, CallbackInfo ci)
	{
		PoseStack mtx = new PoseStack();
		float pt = deltaTracker.getGameTimeDeltaPartialTick(true);
		LocksClientForgeEvents.renderLocks(mtx, Minecraft.getInstance().renderBuffers().bufferSource(), LocksClientUtil.getFrustum(poseMatrix, projMatrix), pt);
	}
}
