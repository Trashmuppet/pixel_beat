# Monochrome Beat — C++ native audio engine.
#
# Phase 2 upgrade: real Oboe / AAudio stream with synthesis, mixing,
# and a lock-free hit-queue. Phase 0 had the JNI surface only.

cmake_minimum_required(VERSION 3.22.1)

project("audio_native"
    LANGUAGES CXX
    VERSION 0.2.0
)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(audio_native SHARED
    AudioEngine.cpp
    Voice.cpp
    Mixer.cpp
)

find_library(log-lib log)

# Oboe ships with the NDK (r23+); link to its shared library if
# available. Build still succeeds in environments without Oboe so
# developers can target older NDKs; the headers then degrade to
# nullptr Oboe usage via #ifdef guards inside AudioEngine.cpp.
find_library(oboe-lib oboe)

target_link_libraries(audio_native
    PRIVATE
        ${log-lib}
)

if(oboe-lib)
    target_link_libraries(audio_native PRIVATE ${oboe-lib})
    target_compile_definitions(audio_native PRIVATE MONOCHROME_BEAT_HAVE_OBOE=1)
else()
    target_compile_definitions(audio_native PRIVATE MONOCHROME_BEAT_HAVE_OBOE=0)
endif()

# Strip debug symbols and hide internal symbols by default.
set_target_properties(audio_native PROPERTIES
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN ON
)
