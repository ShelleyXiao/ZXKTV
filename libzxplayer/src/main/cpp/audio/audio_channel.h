
#ifndef ZXPLAYER_AUDIOCHANNEL_H
#define ZXPLAYER_AUDIOCHANNEL_H


extern "C"
{
#include <libavutil/rational.h>
};



class Channel {
public:
    int channelId = -1;
    AVRational time_base;
    int fps;

public:
    Channel(int id, AVRational base);

    Channel(int id, AVRational base, int fps);
};


#endif //WLPLAYER_WLAUDIOCHANNEL_H
