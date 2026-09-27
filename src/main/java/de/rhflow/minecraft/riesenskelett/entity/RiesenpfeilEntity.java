package de.rhflow.minecraft.riesenskelett.entity;

import de.rhflow.minecraft.riesenskelett.registry.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

/**
 * Riesenpfeil des Riesenskeletts: fliegt wie ein normaler Pfeil (inkl. Schwerkraft),
 * explodiert aber beim Aufkommen mit TNT-Stärke. Kann nicht aufgesammelt werden.
 */
public class RiesenpfeilEntity extends PersistentProjectileEntity {

	/** TNT hat Stärke 4. */
	private static final float EXPLOSION_POWER = 4.0f;

	public RiesenpfeilEntity(EntityType<? extends RiesenpfeilEntity> entityType, World world) {
		super(entityType, world);
	}

	public RiesenpfeilEntity(World world, LivingEntity owner) {
		super(ModEntities.RIESENPFEIL, owner, world, new ItemStack(Items.ARROW), null);
		this.pickupType = PickupPermission.DISALLOWED;
	}

	@Override
	protected ItemStack getDefaultItemStack() {
		return new ItemStack(Items.ARROW);
	}

	/** Nicht das eigene Skelett (oder den Schützen) treffen. */
	@Override
	protected boolean canHit(Entity entity) {
		return !(entity instanceof RiesenskelettEntity) && entity != this.getOwner() && super.canHit(entity);
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		this.explode();
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		this.explode();
	}

	private void explode() {
		if (!this.getWorld().isClient) {
			this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_POWER, World.ExplosionSourceType.TNT);
			this.discard();
		}
	}
}
