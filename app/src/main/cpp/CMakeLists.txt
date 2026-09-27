cmake_minimum_required(VERSION 3.22.1)

project("onecentercore")

# Native Logger library
add_library(
        onecenterlogger
        SHARED
        logger/native_logger.cpp
)

target_include_directories(
        onecenterlogger
        PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/logger
)

target_link_libraries(
        onecenterlogger
        log
)

# Native Core library
add_library(
        onecentercore
        SHARED
        core/native_core.cpp
)

target_include_directories(
        onecentercore
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/logger
        ${CMAKE_CURRENT_SOURCE_DIR}/core
)

target_link_libraries(
        onecentercore
        onecenterlogger
        log
)

# Native Auth library
add_library(
        onecenterauth
        SHARED
        auth/native_auth.cpp
)

target_include_directories(
        onecenterauth
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/logger
        ${CMAKE_CURRENT_SOURCE_DIR}/auth
)

target_link_libraries(
        onecenterauth
        onecenterlogger
        log
)

# Native Security library
#
# native_security_core.cpp/.h contain the pure, JNI-free algorithms and are
# also compiled standalone by security/tests/native_security_core_test.cpp
# for host-side testing (see that file). native_security.cpp is just the
# JNI glue on top. No new link dependency was introduced: this library
# still only needs `log`, same as before.
add_library(
        onecentersecurity
        SHARED
        security/native_security.cpp
        security/native_security_core.cpp
)

target_include_directories(
        onecentersecurity
        PRIVATE
        security
)

target_link_libraries(
        onecentersecurity
        log
)
