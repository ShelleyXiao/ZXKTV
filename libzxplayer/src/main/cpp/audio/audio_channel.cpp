
#include "audio_channel.h"

Channel::Channel(int id, AVRational base) {
    channelId = id;
    time_base = base;
}

Channel::Channel(int id, AVRational base, int f) {
    channelId = id;
    time_base = base;
    fps = f;
}
