package de.rhflow.minecraft.riesenskelett.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Dismounting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.JumpingMount;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * Das Riesenskelett: 20 Blöcke hoch, steht friedlich herum und lässt sich reiten.
 *
 * <ul>
 *   <li>Rechtsklick → man sitzt oben auf dem Kopf und steuert mit WASD + Maus.</li>
 *   <li>Steigt Stufen bis 8 Blöcke hoch ({@code GENERIC_STEP_HEIGHT}).</li>
 *   <li>Leertaste halten und loslassen → Sprung bis 20 Blöcke hoch (Sprungleiste wie beim Pferd).</li>
 *   <li>Fällt es tiefer als 50 Blöcke, reißt es beim Aufschlag einen Krater:
 *       die Blöcke darunter fliegen zur Seite weg ({@link #smashCrater(ServerWorld, float)}).</li>
 *   <li>Schleichen → man steigt ab und landet sicher neben dem Fuß.</li>
 * </ul>
 */
public class RiesenskelettEntity extends PathAwareEntity implements JumpingMount {

	/** Höhe in Blöcken – das Vanilla-Skelett (2 Blöcke) ×10. */
	public static final float HEIGHT = 20.0f;

	/** Start-Geschwindigkeit für einen vollen Sprung: ergibt genau ~20 Blöcke Höhe (Gravitation 0.08, Luftwiderstand 0.98). */
	private static final double FULL_JUMP_VELOCITY = 2.0;

	/** Ab dieser Fallhöhe gibt der Boden nach. */
	private static final float CRATER_FALL_DISTANCE = 50.0f;
	private static final int MAX_CRATER_BLOCKS = 150;

	/** Zum Testen: jeder Sprung reißt beim Landen einen Krater, egal wie hoch. Später wieder auf false. */
	private static final boolean CRATER_AFTER_EVERY_JUMP = true;

	/** Wie weit die Beine pro gelaufenem Block schwingen – kleiner = langsamere, größere Schritte. */
	private static final float LIMB_SWING_PER_BLOCK = 0.5f;

	/** Sprungkraft 0..1, gesetzt über die Sprungleiste; 0 = kein Sprung angefordert. */
	private float jumpStrength;

	/** Nur auf dem Server: wurde seit dem letzten Aufsetzen gesprungen? */
	private boolean jumpedSinceLanding;

	public RiesenskelettEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
		super(entityType, world);
	}

	public static DefaultAttributeContainer.Builder createRiesenskelettAttributes() {
		return PathAwareEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.GENERIC_STEP_HEIGHT, 8.0)
				.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
	}

	// ---------------------------------------------------------------- KI

	@Override
	protected void initGoals() {
		// Kein Herumlaufen – es steht nur da und schaut sich um. Beim Reiten schaut es dorthin, wo der Reiter hinschaut.
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f) {
			@Override
			public boolean canStart() {
				return !RiesenskelettEntity.this.hasPassengers() && super.canStart();
			}

			@Override
			public boolean shouldContinue() {
				return !RiesenskelettEntity.this.hasPassengers() && super.shouldContinue();
			}
		});
		this.goalSelector.add(2, new LookAroundGoal(this) {
			@Override
			public boolean canStart() {
				return !RiesenskelettEntity.this.hasPassengers() && super.canStart();
			}

			@Override
			public boolean shouldContinue() {
				return !RiesenskelettEntity.this.hasPassengers() && super.shouldContinue();
			}
		});
	}

	/** Verschwindet nie von selbst. */
	@Override
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	/** Kein Ersticken, wenn der Kopf in Blättern steckt, und kein Fallschaden. */
	@Override
	public boolean isInvulnerableTo(DamageSource damageSource) {
		return damageSource.isOf(DamageTypes.IN_WALL)
				|| damageSource.isOf(DamageTypes.FALL)
				|| damageSource.isOf(DamageTypes.CRAMMING)
				|| super.isInvulnerableTo(damageSource);
	}

	// ---------------------------------------------------------------- Reiten

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (player.isSneaking() || this.hasPassengers()) {
			return super.interactMob(player, hand);
		}
		if (!this.getWorld().isClient) {
			player.startRiding(this);
		}
		return ActionResult.success(this.getWorld().isClient);
	}

	@Nullable
	@Override
	public LivingEntity getControllingPassenger() {
		if (this.getFirstPassenger() instanceof PlayerEntity player) {
			return player;
		}
		return super.getControllingPassenger();
	}

	@Override
	protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {
		super.tickControlled(controllingPlayer, movementInput);
		// Blickrichtung vom Reiter übernehmen. Der Kopf nickt nicht mit, sonst kippt er dem Reiter vors Gesicht.
		this.setRotation(controllingPlayer.getYaw(), 0.0f);
		this.prevYaw = this.bodyYaw = this.headYaw = this.getYaw();

		if (this.isOnGround()) {
			if (this.isLogicalSideForUpdatingMovement() && this.jumpStrength > 0.0f) {
				this.giantJump(this.jumpStrength, movementInput);
			}
			this.jumpStrength = 0.0f;
		}
	}

	@Override
	protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
		float sideways = controllingPlayer.sidewaysSpeed * 0.5f;
		float forward = controllingPlayer.forwardSpeed;
		if (forward <= 0.0f) {
			forward *= 0.25f;
		}
		return new Vec3d(sideways, 0.0, forward);
	}

	@Override
	protected float getSaddledSpeed(PlayerEntity controllingPlayer) {
		return (float) this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
	}

	@Override
	public void tick() {
		super.tick();
		// Der Reiter sitzt 20 Blöcke hoch – bei Sprüngen und Stürzen soll ihm nichts passieren.
		for (Entity passenger : this.getPassengerList()) {
			passenger.onLanding();
		}
	}

	/** Beim Absteigen nicht vom Kopf fallen, sondern sicher neben dem Fuß landen. */
	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		float[] angles = {90.0f, -90.0f, 0.0f, 180.0f, 45.0f, -45.0f, 135.0f, -135.0f};
		for (float angle : angles) {
			Vec3d offset = getPassengerDismountOffset(this.getWidth(), passenger.getWidth(), this.getYaw() + angle);
			Vec3d spot = this.findDismountSpot(this.getX() + offset.x, this.getZ() + offset.z, passenger);
			if (spot != null) {
				return spot;
			}
		}
		// Notlösung: direkt zwischen die Füße (die Hitbox des Skeletts blockiert Spieler nicht).
		return new Vec3d(this.getX(), this.getBoundingBox().minY, this.getZ());
	}

	/** Sucht von Fußhöhe aus nach oben (bis Stufenhöhe) einen freien Platz mit Boden, sonst ein Stück nach unten. */
	@Nullable
	private Vec3d findDismountSpot(double x, double z, LivingEntity passenger) {
		int footY = MathHelper.floor(this.getBoundingBox().minY);
		for (int y = footY - 1; y <= footY + 8; y++) {
			Vec3d spot = this.dismountSpotAt(x, y, z, passenger);
			if (spot != null) {
				return spot;
			}
		}
		for (int y = footY - 2; y >= footY - 24; y--) {
			Vec3d spot = this.dismountSpotAt(x, y, z, passenger);
			if (spot != null) {
				return spot;
			}
		}
		return null;
	}

	@Nullable
	private Vec3d dismountSpotAt(double x, int y, double z, LivingEntity passenger) {
		BlockPos pos = BlockPos.ofFloored(x, y, z);
		double height = this.getWorld().getDismountHeight(pos);
		if (!Dismounting.canDismountInBlock(height)) {
			return null;
		}
		Vec3d spot = new Vec3d(x, y + height, z);
		Box box = passenger.getBoundingBox(passenger.getPose()).offset(spot);
		return Dismounting.canPlaceEntityAt(this.getWorld(), passenger, box) ? spot : null;
	}

	// ---------------------------------------------------------------- Springen (Sprungleiste wie beim Pferd)

	@Override
	public void setJumpStrength(int strength) {
		if (strength <= 0) {
			return;
		}
		// Wie beim Pferd: ab 90 % Ladung volle Kraft, sonst 40..80 %.
		this.jumpStrength = strength >= 90 ? 1.0f : 0.4f + 0.4f * strength / 90.0f;
	}

	@Override
	public boolean canJump() {
		return this.hasPassengers();
	}

	@Override
	public void startJumping(int height) {
		// Wird auf dem Server aufgerufen, wenn der Reiter die Leertaste loslässt.
		this.jumpedSinceLanding = true;
		this.playSound(SoundEvents.ENTITY_SKELETON_STEP, 2.0f, 0.4f);
	}

	@Override
	public void stopJumping() {
	}

	private void giantJump(float strength, Vec3d movementInput) {
		Vec3d velocity = this.getVelocity();
		this.setVelocity(velocity.x, FULL_JUMP_VELOCITY * strength, velocity.z);
		// Beim Vorwärtslaufen etwas Schwung in Blickrichtung mitnehmen.
		if (movementInput.z > 0.0) {
			float sin = MathHelper.sin(this.getYaw() * MathHelper.RADIANS_PER_DEGREE);
			float cos = MathHelper.cos(this.getYaw() * MathHelper.RADIANS_PER_DEGREE);
			this.setVelocity(this.getVelocity().add(-0.8 * sin * strength, 0.0, 0.8 * cos * strength));
		}
		this.velocityDirty = true;
	}

	// ---------------------------------------------------------------- Aufschlag + Krater

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		boolean jumped = this.jumpedSinceLanding;
		this.jumpedSinceLanding = false;
		boolean smash = fallDistance > CRATER_FALL_DISTANCE || (CRATER_AFTER_EVERY_JUMP && jumped);
		if (smash && this.getWorld() instanceof ServerWorld serverWorld) {
			this.smashCrater(serverWorld, fallDistance);
		}
		return false; // Das Skelett selbst nimmt keinen Fallschaden.
	}

	/**
	 * Lässt den Boden unter den Füßen nachgeben: alle Blöcke in einer flachen Halbkugel werden zu
	 * fliegenden Blöcken, die vom Aufschlagpunkt weg zur Seite (und etwas nach oben) geschleudert werden.
	 */
	private void smashCrater(ServerWorld world, float fallDistance) {
		BlockPos center = BlockPos.ofFloored(this.getX(), this.getBoundingBox().minY - 0.5, this.getZ());
		Vec3d impact = Vec3d.ofCenter(center);

		world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y + 1.0, impact.z, 1, 0.0, 0.0, 0.0, 0.0);
		world.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
				SoundCategory.NEUTRAL, 6.0f, 0.6f);

		if (!world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
			return;
		}

		// Je tiefer der Sturz, desto größer der Krater (gedeckelt).
		int radius = Math.min(5 + (int) (Math.max(fallDistance - CRATER_FALL_DISTANCE, 0.0f) / 25.0f), 8);
		int depth = 3;

		List<BlockPos> candidates = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dy = -depth + 1; dy <= 0; dy++) {
					double shape = (double) (dx * dx + dz * dz) / (radius * radius) + (double) (dy * dy) / (depth * depth);
					if (shape > 1.0) {
						continue;
					}
					BlockPos pos = center.add(dx, dy, dz);
					if (this.canBeSmashed(world, pos)) {
						candidates.add(pos);
					}
				}
			}
		}
		candidates.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(center)));

		int count = Math.min(candidates.size(), MAX_CRATER_BLOCKS);
		for (int i = 0; i < count; i++) {
			BlockPos pos = candidates.get(i);
			FallingBlockEntity block = FallingBlockEntity.spawnFromBlock(world, pos, world.getBlockState(pos));

			Vec3d away = Vec3d.ofCenter(pos).subtract(impact);
			Vec3d horizontal = new Vec3d(away.x, 0.0, away.z);
			double distance = horizontal.length();
			if (distance < 0.01) {
				// Genau unter dem Fuß: in eine zufällige Richtung.
				float angle = this.random.nextFloat() * MathHelper.TAU;
				horizontal = new Vec3d(MathHelper.cos(angle), 0.0, MathHelper.sin(angle));
			} else {
				horizontal = horizontal.multiply(1.0 / distance);
			}
			// Nah am Zentrum fliegt es weiter.
			double push = 0.6 + 0.8 * (1.0 - distance / (radius + 1)) + this.random.nextDouble() * 0.3;
			double lift = 0.5 + this.random.nextDouble() * 0.5;
			block.setVelocity(horizontal.x * push, lift, horizontal.z * push);
			block.velocityModified = true;
		}
	}

	private boolean canBeSmashed(ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return !state.isAir()
				&& state.getFluidState().isEmpty()
				&& !state.hasBlockEntity()
				&& state.getHardness(world, pos) >= 0.0f; // Grundgestein & Co. bleiben
	}

	// ---------------------------------------------------------------- Animation + Sounds

	/** Riesenschritte: die Beine schwingen pro gelaufenem Block viel langsamer als beim normalen Skelett. */
	@Override
	protected void updateLimbs(float posDelta) {
		this.limbAnimator.updateLimbs(Math.min(posDelta * LIMB_SWING_PER_BLOCK, 1.0f), 0.4f);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SKELETON_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.ENTITY_SKELETON_STEP, 1.5f, 0.5f);
	}

	@Override
	protected float getSoundVolume() {
		return 2.0f;
	}

	/** Tiefere Stimme – klingt groß. */
	@Override
	public float getSoundPitch() {
		return (this.random.nextFloat() - this.random.nextFloat()) * 0.1f + 0.5f;
	}
}
