package de.rhflow.minecraft.riesenskelett.client;

import de.rhflow.minecraft.riesenskelett.entity.RiesenskelettEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Zeichnet das Riesenskelett mit dem eingebauten Skelett-Modell und der Original-Textur, nur ×10 vergrößert.
 */
public class RiesenskelettRenderer extends MobEntityRenderer<RiesenskelettEntity, RiesenskelettRenderer.GiantSkeletonModel> {
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/skeleton/skeleton.png");

	/** Vanilla-Skelett ist 2 Blöcke hoch → ×10 = 20 Blöcke. */
	private static final float SCALE = 10.0f;

	public RiesenskelettRenderer(EntityRendererFactory.Context context) {
		super(context, new GiantSkeletonModel(context.getPart(EntityModelLayers.SKELETON)), 3.0f);
	}

	@Override
	protected void scale(RiesenskelettEntity entity, MatrixStack matrices, float amount) {
		matrices.scale(SCALE, SCALE, SCALE);
	}

	@Override
	public Identifier getTexture(RiesenskelettEntity entity) {
		return TEXTURE;
	}

	/**
	 * Das Vanilla-Skelett-Modell (dünne Arme und Beine). Weil die Beine pro Block langsamer schwingen
	 * (siehe {@code RiesenskelettEntity#updateLimbs}), wird der Ausschlag hier wieder verstärkt –
	 * sonst würde der Riese nur trippeln.
	 */
	public static class GiantSkeletonModel extends BipedEntityModel<RiesenskelettEntity> {
		private static final float SWING_BOOST = 2.5f;

		public GiantSkeletonModel(ModelPart root) {
			super(root);
		}

		@Override
		public void setAngles(RiesenskelettEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
			super.setAngles(entity, limbAngle, Math.min(limbDistance * SWING_BOOST, 1.0f), animationProgress, headYaw, headPitch);
		}
	}
}
