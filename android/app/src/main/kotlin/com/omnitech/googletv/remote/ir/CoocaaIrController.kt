package com.omnitech.googletv.remote.ir

import android.content.Context
import android.hardware.ConsumerIrManager

class CoocaaIrController(private val context: Context) {

    // ConsumerIrManager service ko initialize karna
    private val irManager: ConsumerIrManager? by lazy {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    }

    // Check karein ki phone me IR blaster available hai ya nahi
    fun hasIrBlaster(): Boolean {
        return irManager?.hasIrEmitter() == true
    }

    companion object {
        // Coocaa / Skyworth TV ki standard carrier frequency (Hz me)
        const val CARRIER_FREQUENCY = 38400

        // Coocaa TV ke Power button ka IR timing pattern (microseconds me)
        val COOCAA_POWER_PATTERN = intArrayOf(
            1901, 4453, 625, 1614, 625, 1588, 625, 1614, 625, 442,
            625, 442, 625, 468, 625, 442, 625, 494, 572, 1614,
            625, 1588, 625, 1614, 625, 494, 572, 442, 651, 442,
            625, 442, 625, 442, 625, 1614, 625, 1588, 651, 1588,
            625, 442, 625, 494, 598, 442, 625, 442, 625, 520,
            572, 442, 625, 442, 625, 442, 651, 1588, 625, 1614,
            625, 1588, 625, 1614, 625, 1588, 625, 48958
        )
    }

    // Coocaa TV ko Power ON karne ka function
    fun sendPowerOnSignal(): Boolean {
        if (!hasIrBlaster()) {
            // Agar phone me IR sensor nahi hai
            return false
        }

        // IR signal ko air me transmit karna
        irManager?.transmit(CARRIER_FREQUENCY, COOCAA_POWER_PATTERN)
        return true
    }
}
