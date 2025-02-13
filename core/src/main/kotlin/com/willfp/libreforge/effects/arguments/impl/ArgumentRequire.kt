package com.willfp.libreforge.effects.arguments.impl

import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.effects.ElementLike
import com.willfp.libreforge.effects.arguments.EffectArgument
import com.willfp.libreforge.getDoubleFromExpression
import com.willfp.libreforge.triggers.DispatchedTrigger

object ArgumentRequire : EffectArgument<NoCompileData>("require") {
    override fun isMet(element: ElementLike, trigger: DispatchedTrigger, compileData: NoCompileData): Boolean {
        return element.config.getDoubleFromExpression("require", trigger.data) == 1.0
    }
}
