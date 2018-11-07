

#ifndef PLAYER_BASEPLAYER_H
#define PLAYER_BASEPLAYER_H

extern "C"
{
#include <libavcodec/avcodec.h>
};

class ZXbasePlayer {

public:
    int streamIndex;
    int duration;
    double clock = 0;
    double now_time = 0;
    AVCodecContext *avCodecContext = NULL;
    AVRational time_base;

public:
    ZXbasePlayer();
    ~ZXbasePlayer();
};


#endif //WLPLAYER_WLBASEPLAYER_H
