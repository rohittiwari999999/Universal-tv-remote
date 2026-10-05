package com.omnitech.universal.tv.remote

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.omnitech.universal.tv.remote/consumer_ir"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            val irManager = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

            when (call.method) {
                "hasIrEmitter" -> {
                    val hasEmitter = irManager?.hasIrEmitter() ?: false
                    result.success(hasEmitter)
                }
                "getCarrierFrequencies" -> {
                    if (irManager != null && irManager.hasIrEmitter()) {
                        val ranges = irManager.carrierFrequencies?.map { "${it.minFrequency}-${it.maxFrequency}" } ?: listOf("38000-38000")
                        result.success(ranges)
                    } else {
                        result.success(emptyList<String>())
                    }
                }
                "transmit" -> {
                    val freq = call.argument<Int>("frequency") ?: 38000
                    val patternList = call.argument<List<Int>>("pattern")
                    if (irManager != null && irManager.hasIrEmitter() && patternList != null) {
                        try {
                            val pattern = patternList.toIntArray()
                            irManager.transmit(freq, pattern)
                            result.success(true)
                        } catch (e: Exception) {
                            result.error("IR_TRANSMIT_ERROR", e.localizedMessage, null)
                        }
                    } else {
                        result.success(false)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}
