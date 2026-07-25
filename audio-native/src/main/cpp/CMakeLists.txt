# Monochrome Beat — C++ native engine build script.
#
# Phase 0 makes the JNI surface + CMake wiring real so the multi-ABI
# externalNativeBuild step stays green. The real Oboe / AAudio signal
# graph lands in Phase 2 (Native drum synthesis).

cmake_minimum_required(VERSION 3.22.1)

project("audio_native"
    LANGUAGES CXX
    VERSION 0.1.0
)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(audio_native SHARED
    audio_engine.cpp
)

# Public link dependencies consumed by the JNI surface.
find_library(log-lib log)
target_link_libraries(audio_native
    PRIVATE
        ${log-lib}
)

# Strip debug symbols in Release for smaller APKs.
set_target_properties(audio_native PROPERTIES
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN ON
)
