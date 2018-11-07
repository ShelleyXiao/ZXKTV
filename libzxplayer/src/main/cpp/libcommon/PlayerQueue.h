

#ifndef PLAYER_QUEUE_H
#define PLAYER_QUEUE_H

#include "queue"
#include "../PlayStatus.h"

extern "C"
{
#include <libavcodec/avcodec.h>
#include "pthread.h"
};

class PlayerQueue {

public:
    std::queue<AVPacket*> queuePacket;
    std::queue<AVFrame*> queueFrame;
    pthread_mutex_t mutexFrame;
    pthread_cond_t condFrame;
    pthread_mutex_t mutexPacket;
    pthread_cond_t condPacket;
    PlayStatus *wlPlayStatus = NULL;

public:
    PlayerQueue(PlayStatus *playStatus);
    ~PlayerQueue();
    int putAvpacket(AVPacket *avPacket);
    int getAvpacket(AVPacket *avPacket);
    int clearAvpacket();
    int clearToKeyFrame();

    int putAvframe(AVFrame *avFrame);
    int getAvframe(AVFrame *avFrame);
    int clearAvFrame();

    void release();
    int getAvPacketSize();
    int getAvFrameSize();

    int noticeThread();
};


#endif //PLAYER_QUEUE_H
