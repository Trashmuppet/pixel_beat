// Monochrome Beat — lock-free single-producer / single-consumer ring
// buffer for scheduling drum hits into the audio thread.
//
// `push` is called from the realtime transport on the Kotlin / JNI
// thread. `pop` is called from the liboboe / AAudio render callback.
//
// The buffer is fixed-size, no allocation on the audio thread, and
// lock-free by construction. Capacity is a power of two so we can
// compute indices via bitmask.
#pragma once

#include <array>
#include <atomic>
#include <cstddef>
#include <cstdint>

namespace monochrome_beat::audio {

struct TriggerEvent {
    int64_t  sampleTick;  // absolute sample position in the timeline
    int32_t  voiceIndex;  // which voice to fire
    float    velocity;    // 0 .. 1
};

class HitQueue {
public:
    static constexpr size_t kCapacity = 4096;

    void push(const TriggerEvent& event) noexcept {
        const size_t head = head_.load(std::memory_order_relaxed);
        const size_t next = (head + 1) & (kCapacity - 1);
        // Drop on overflow — log on the audio thread would be too
        // expensive, so producers must size their bursts to capacity.
        if (next != tail_.load(std::memory_order_acquire)) {
            ring_[head] = event;
            head_.store(next, std::memory_order_release);
        }
    }

    bool pop(TriggerEvent& out) noexcept {
        const size_t tail = tail_.load(std::memory_order_relaxed);
        if (tail == head_.load(std::memory_order_acquire)) {
            return false;
        }
        out = ring_[tail];
        tail_.store((tail + 1) & (kCapacity - 1), std::memory_order_release);
        return true;
    }

    void reset() noexcept {
        head_.store(0, std::memory_order_relaxed);
        tail_.store(0, std::memory_order_relaxed);
    }

private:
    std::array<TriggerEvent, kCapacity> ring_{};
    alignas(64) std::atomic<size_t> head_{0};
    alignas(64) std::atomic<size_t> tail_{0};
};

}  // namespace monochrome_beat::audio
