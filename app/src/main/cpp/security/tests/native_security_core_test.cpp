// Standalone HOST test for NativeSecurity's core primitives.
//
// This is NOT wired into the Android Gradle/CMake build — it deliberately
// has zero dependency on jni.h or the Android NDK, so it can be compiled
// and run directly on your workstation to sanity-check the algorithm in
// seconds, without a device/emulator. The real on-device behavior is
// covered separately by the JVM tests (SecurityStateTest, IntegrityHashTest)
// and the instrumented tests (NativeSecurityInstrumentedTest), which
// exercise this same logic through the actual JNI path.
//
// Build & run (from this directory):
//   g++ -std=c++17 -Wall -Wextra -O0 native_security_core_test.cpp
//       ../native_security_core.cpp -o native_security_core_test
//   ./native_security_core_test
// (the build command is one line; wrapped above only for width)
//
// Expected output: "ALL TESTS PASSED (0 failure(s))" and exit code 0.

#include "../native_security_core.h"
#include <iostream>
#include <string>

namespace {

int failures = 0;

void check(bool condition, const std::string& name) {
    if (condition) {
        std::cout << "[PASS] " << name << "\n";
    } else {
        std::cout << "[FAIL] " << name << "\n";
        ++failures;
    }
}

} // namespace

int main() {
    using NS = NativeSecurity;

    // ---- constantTimeEquals ----
    check(NS::constantTimeEquals("abc", "abc"), "constantTimeEquals: equal values");
    check(!NS::constantTimeEquals("abc", "abd"), "constantTimeEquals: different values, same length");
    check(!NS::constantTimeEquals("abc", "ab"), "constantTimeEquals: different lengths");
    check(NS::constantTimeEquals("", ""), "constantTimeEquals: empty values");
    check(!NS::constantTimeEquals("abc", ""), "constantTimeEquals: one empty, one not");
    check(NS::constantTimeEquals(
              "00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF",
              "00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF"),
          "constantTimeEquals: digest-shaped strings, equal");
    check(!NS::constantTimeEquals(
              "00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF",
              "00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:00"),
          "constantTimeEquals: digest-shaped strings, differ in last byte");

    // ---- secureClear ----
    {
        std::string value = "sensitive";
        NS::secureClear(value);
        check(value.empty(), "secureClear: clears and empties the string");
    }
    {
        std::string value; // already empty
        NS::secureClear(value);
        check(value.empty(), "secureClear: no-op on already-empty string does not crash");
    }

    std::cout << "\n" << (failures == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED")
              << " (" << failures << " failure(s))\n";
    return failures == 0 ? 0 : 1;
}
