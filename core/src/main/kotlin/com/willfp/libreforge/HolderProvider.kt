package com.willfp.libreforge

import com.github.benmanes.caffeine.cache.Caffeine
import com.willfp.libreforge.effects.EffectBlock
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Provides the holders that are held by a player.
 */
interface HolderProvider {
    /**
     * Provide the holders.
     */
    fun provide(player: Player): Collection<ProvidedHolder<*>>
}

class HolderProvideEvent(
    who: Player,
    val holders: Collection<ProvidedHolder<*>>
) : PlayerEvent(who) {
    override fun getHandlers() = handlerList

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
    }
}

private val providers = mutableListOf<HolderProvider>()

/**
 * Register a new holder provider.
 */
fun registerHolderProvider(provider: HolderProvider) = providers.add(provider)

/**
 * Register a new holder provider.
 */
fun registerHolderProvider(provider: (Player) -> Collection<ProvidedHolder<*>>) =
    providers.add(object : HolderProvider {
        override fun provide(player: Player) = provider(player)
    })

private val holderCache = Caffeine.newBuilder()
    .expireAfterWrite(4, TimeUnit.SECONDS)
    .build<UUID, Collection<ProvidedHolder<*>>>()

/**
 * The holders.
 */
val Player.holders: Collection<ProvidedHolder<*>>
    get() = holderCache.get(this.uniqueId) {
        val holders = providers.flatMap { it.provide(this) }

        Bukkit.getPluginManager().callEvent(
            HolderProvideEvent(this, holders)
        )

        holders
    }

/**
 * Invalidate holder cache to force rescan.
 */
fun Player.updateHolders() {
    holderCache.invalidate(this.uniqueId)
}

// Effects that were active on previous update
private val previousStates = DefaultHashMap<UUID, Map<Set<EffectBlock>, ProvidedHolder<*>>>(emptyMap())
private val flattenedPreviousStates = DefaultHashMap<UUID, Set<EffectBlock>>(emptySet()) // Optimisation.

/**
 * Flatten down to purely the effects.
 */
fun Map<Set<EffectBlock>, ProvidedHolder<*>>.flatten() = this.flatMap { it.key }.toSet()

/**
 * Get active effects for a [player] from holders mapped to the holder
 * that has provided them.
 */
fun Collection<ProvidedHolder<*>>.getProvidedActiveEffects(player: Player): Map<Set<EffectBlock>, ProvidedHolder<*>> {
    val map = mutableMapOf<Set<EffectBlock>, ProvidedHolder<*>>()

    for (holder in this) {
        val effects = holder.holder.getActiveEffects(player)
        map[effects] = holder
    }

    return map
}

/**
 * Get active effects for a [player] from holders.
 */
fun Collection<ProvidedHolder<*>>.getActiveEffects(player: Player) =
    this.map { it.holder }
        .getActiveEffects(player)

/**
 * Get active effects for a [player] from holders.
 */
fun Collection<Holder>.getActiveEffects(player: Player) =
    this.filter { it.conditions.areMet(player) }
        .flatMap { it.getActiveEffects(player) }
        .toSet()

/**
 * Get active effects for a [player].
 */
fun Holder.getActiveEffects(player: Player) =
    this.effects.filter { it.conditions.areMet(player) }.toSet()

/**
 * Recalculate active effects.
 */
fun Player.calculateActiveEffects() =
    this.holders.getProvidedActiveEffects(this)

/**
 * The active effects.
 */
val Player.activeEffects: Set<EffectBlock>
    get() = flattenedPreviousStates[this.uniqueId]

/**
 * The active effects mapped to the holder that provided them.
 */
val Player.providedActiveEffects: Map<Set<EffectBlock>, ProvidedHolder<*>>
    get() = previousStates[this.uniqueId]

/**
 * Update the active effects.
 */
fun Player.updateEffects() {
    val before = this.providedActiveEffects
    val after = this.calculateActiveEffects()

    previousStates[this.uniqueId] = after
    flattenedPreviousStates[this.uniqueId] = after.flatten()

    val beforeF = before.flatten()
    val afterF = after.flatten()

    val added = afterF - beforeF
    val removed = beforeF - afterF

    for (effect in removed) {
        effect.disable(this)
    }

    for (effect in added) {
        effect.enable(this)
    }

    for (effect in afterF) {
        effect.reload(this)
    }
}
