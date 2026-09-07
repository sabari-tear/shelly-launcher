package com.shalltear.shellylauncher.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a Baseline Profile for ShellyLauncher.
 *
 * Run via:
 *   ./gradlew :macrobenchmark:connectedBenchmarkAndroidTest \
 *       -P android.testInstrumentationRunnerArguments.class=\
 *         com.shalltear.shellylauncher.benchmark.BaselineProfileGenerator
 *
 * The generated profile is written to the connected device and can then be
 * pulled and checked into app/src/main/baseline-prof.txt so profileinstaller
 * packages it in the APK for warm/cold-start speedups on Android 9+ devices.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.shalltear.shellylauncher",
    ) {
        // Cover the critical startup path and the honeycomb grid interaction.
        pressHome()
        startActivityAndWait()
        val cx = device.displayWidth / 2
        val cy = device.displayHeight / 2
        device.drag(cx, cy, cx, cy, 100)
        device.pressBack()
    }
}
