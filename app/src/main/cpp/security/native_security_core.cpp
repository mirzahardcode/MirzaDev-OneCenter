#include "native_security_core.h"
#include <algorithm>
#include <cstddef>

bool NativeSecurity::constantTimeEquals(const std::string& a, const std::string& b) {
    // XOR-accumulate over the full length of both inputs so total
    // execution time depends only on max(len(a), len(b)), never on where
    // (or whether) the first mismatching byte occurs.
    volatile unsigned char diff = 0;
    diff |= static_cast<unsigned char>(a.length() ^ b.length());

    const size_t minLen = std::min(a.length(), b.length());
    for (size_t i = 0; i < minLen; ++i) {
        diff |= static_cast<unsigned char>(a[i] ^ b[i]);
    }

    // Keep touching the longer buffer past minLen so a length mismatch
    // doesn't shorten the loop in a way that could be distinguished from
    // a length match by an observer timing only this loop.
    const size_t maxLen = std::max(a.length(), b.length());
    for (size_t i = minLen; i < maxLen; ++i) {
        const unsigned char val = (i < a.length())
            ? static_cast<unsigned char>(a[i])
            : static_cast<unsigned char>(b[i]);
        diff |= val;
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
