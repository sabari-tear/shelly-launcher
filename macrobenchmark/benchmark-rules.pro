# Keep all benchmark test classes and their members so the test runner can find them.
-keep class com.shalltear.shellylauncher.benchmark.** { *; }

# Keep JUnit runner infrastructure.
-keep class androidx.test.** { *; }
-keep class org.junit.** { *; }
