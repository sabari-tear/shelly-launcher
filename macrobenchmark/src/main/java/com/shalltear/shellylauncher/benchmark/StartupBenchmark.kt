package com.shalltear.shellylauncher.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.shalltear.shellylauncher"

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Measures cold-start time with no ahead-of-time compilation.
     * This is the worst-case scenario — closest to a fresh install on a low-end device.
     * Expect 400–1000 ms on API 26 hardware.
     */
    @Test
    fun coldStartNoCompilation() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.None(),
    ) {
        pressHome()
        startActivityAndWait()
    }

    /**
     * Measures cold-start time after a Baseline Profile has been applied.
     * CompilationMode.Partial() uses whatever profile is installed in the APK.
     * Compare with [coldStartNoCompilation] to quantify the profile benefit.
     */
    @Test
    fun coldStartWithBaselineProfile() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.Partial(),
    ) {
        pressHome()
        startActivityAndWait()
    }

    /**
     * Measures frame timing while the user long-presses to reveal the honeycomb app grid.
     * A P99 frame time > 16 ms indicates jank on the low-end target device (60 Hz).
     * Run with a physical API 26 device for representative results.
     */
    @Test
    fun appGridFrameTiming() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 3,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Full(),
    ) {
        pressHome()
        startActivityAndWait()
        // Simulate a long-press (400 ms threshold) at screen centre to open the grid.
        // 100 UiAutomator drag steps ≈ 500 ms at the default 5 ms/step rate.
        val cx = device.displayWidth / 2
        val cy = device.displayHeight / 2
        device.drag(cx, cy, cx, cy, 100)
        device.pressBack()
    }
}
