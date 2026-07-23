package dev.lumas.biomes.enums;

import dev.wyck.environment.particle.ParticleData;
import dev.wyck.environment.particle.options.BlockParticle;
import dev.wyck.environment.particle.options.ColorParticle;
import dev.wyck.environment.particle.options.DustParticle;
import dev.wyck.environment.particle.options.DustTransitionParticle;
import dev.wyck.environment.particle.options.ItemParticle;
import dev.wyck.environment.particle.options.PowerParticle;
import dev.wyck.environment.particle.options.SculkChargeParticle;
import dev.wyck.environment.particle.options.SpellParticle;
import org.bukkit.Material;

public enum SimpleParticleData {
    BLOCK(Material.class, BlockParticle.class, BlockParticle::of),
    COLOR(String.class, ColorParticle.class, ColorParticle::of),
    DUST(String.class, DustParticle.class, DustParticle::of),
    DUST_TRANSITION(String.class, DustTransitionParticle.class, ctx -> {
        String[] colors = ctx.split(",");
        if (colors.length != 2) {
            throw new IllegalArgumentException("Invalid dust transition particle data: " + ctx);
        }
        return DustTransitionParticle.of(colors[0], colors[1]);
    }),
    ITEM(Material.class, ItemParticle.class, ItemParticle::of),
    POWER(float.class, PowerParticle.class, PowerParticle::of),
    SCULK_CHARGE(float.class, SculkChargeParticle.class, SculkChargeParticle::of),
    SPELL(String.class, SpellParticle.class, SpellParticle::of),
    // TODO: Support multiple args
    //TRAIL
    //VIBRATION
    ;

    private final Class<?> contextType;
    private final Class<? extends ParticleData> particleDataClass;
    private final ParticleDataFactory<?, ?> factory;

    <T, R extends ParticleData> SimpleParticleData(Class<T> contextType, Class<R> particleDataClass, ParticleDataFactory<T, R> factory) {
        this.contextType = contextType;
        this.particleDataClass = particleDataClass;
        this.factory = factory;
    }


    public ParticleData create(String context) {
        Object parsedContext;
        if (contextType == Material.class) {
            parsedContext = Material.valueOf(context);
        } else if (contextType == float.class) {
            parsedContext = Float.parseFloat(context);
        } else if (contextType == String.class) {
            parsedContext = context;
        } else {
            throw new IllegalStateException("Unsupported context type: " + contextType);
        }
        return ((ParticleDataFactory<Object, ParticleData>) factory).create(parsedContext);
    }

    public static <R extends ParticleData> SimpleParticleData fromParticleData(Class<R> particleDataClass) {
        for (SimpleParticleData value : values()) {
            if (value.particleDataClass.equals(particleDataClass)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No SimpleParticleData found for class: " + particleDataClass);
    }


    interface ParticleDataFactory<T, R extends ParticleData> {
        R create(T context);
    }
}
