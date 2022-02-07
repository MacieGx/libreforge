package com.willfp.libreforge.chains

import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.effects.Effects

@Suppress("UNUSED")
object EffectChains {
    private val BY_ID = HashBiMap.create<String, EffectChain>()

    /**
     * Get chain matching id.
     *
     * @param id The id to query.
     * @return The matching chain, or null if not found.
     */
    fun getByID(id: String): EffectChain? {
        return BY_ID[id]
    }

    /**
     * List of all registered chains.
     *
     * @return The chains.
     */
    fun values(): List<EffectChain> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    /**
     * Compile an effect chain.
     *
     * @param config The config for the effect chain.
     * @param context The context to log violations for.
     *
     * @return The effect chain, or null if invalid.
     */
    @JvmStatic
    fun compile(config: Config, context: String, anonymous: Boolean = false): EffectChain? {
        val id = if (anonymous) "anonymous" else config.getString("id")

        val components = mutableListOf<ChainComponent>()

        config.getSubsections("effects").mapNotNull {
            Effects.compile(it, "$context (Chain ID $id)", fromChain = true)
        }.mapTo(components) { ChainComponentEffect(it) }

        if (components.isEmpty()) {
            return null
        }

        val chain = EffectChain(id, components)

        if (!anonymous) {
            BY_ID[id] = chain
        }

        return chain
    }
}
