package de.rhflow.minecraft.riesenskelett.client;

import de.rhflow.minecraft.riesenskelett.entity.RiesenpfeilEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Der normale Pfeil – nur 10× so groß, passend zum Riesenskelett. */
public class RiesenpfeilRenderer extends ProjectileEntityRenderer<RiesenpfeilEntity> {
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/projectiles/arrow.png");
	private static final float SCALE = 10.0f;

	public RiesenpfeilRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void render(RiesenpfeilEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		matrices.scale(SCALE, SCALE, SCALE);
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
		matrices.pop();
	}

	@Override
	public Identifier getTexture(RiesenpfeilEntity entity) {
		return TEXTURE;
	}
}
