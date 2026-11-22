#include "native_security_core.h"
#include <algorithm>
#include <cstddef>

bool NativeSecurity::constantTimeEquals(const std::string& a, const std::string& b) {
    const size_t lenA = a.length();
    const size_t lenB = b.length();

    // Accumulate length difference in diff byte.
    // If lengths differ, diff will be non-zero (ensuring the result is false).
    volatile unsigned char diff = static_cast<unsigned char>(lenA ^ lenB);

    // Byte-by-byte comparison up to min(lenA, lenB).
    // For fixed-size hash comparisons (e.g. SHA-256 digests), lenA == lenB,
    // so this loop compares every byte without early returns or conditional branches.
    const size_t minLen = (lenA < lenB) ? lenA : lenB;
    for (size_t i = 0; i < minLen; ++i) {
        diff |= static_cast<unsigned char>(a[i] ^ b[i]);
    }

    return diff == 0;
}

void NativeSecurity::secureClear(std::string& value) {
    if (value.empty()) {
        return;
    }

    // volatile prevents the compiler from proving this write is dead (the
    // string is about to be cleared/destroyed) and eliding it.
    volatile char* p = &value[0];
    size_t len = value.length();
    while (len--) {
        *p++ = 0;
    }

    value.clear();
    // Best-effort: ask the implementation to release the (now zeroed)
    // heap buffer instead of retaining it as spare capacity. Non-binding
    // per the standard, but harmless, and there's no reason to keep a
    // zeroed secret-shaped buffer resident.
    value.shrink_to_fit();
}
