

#ifndef WLPLAYER_WLVIDEO_H
#define WLPLAYER_WLVIDEO_H


#include "../base_player.h"
#include "WlQueue.h"
#include "../JavaJNICallback.h"
#include "AndroidLog.h"
#include "audio_decoder.h"

extern "C"
{
    #include <libavutil/time.h>
};

class VideoDecoder : public ZXbasePlayer{

public:
    WlQueue *queue = NULL;
    AudioDecoder *audioDecoder = NULL;
    PlayStatus *wlPlayStatus = NULL;
    pthread_t videoThread;
    pthread_t decFrame;
    JavaJNICallback *javaJNICall = NULL;

    double delayTime = 0;
    int rate = 0;
    bool isExit = true;
    bool isExit2 = true;
    int codecType = -1;
    double video_clock = 0;
    double framePts = 0;
    bool frameratebig = false;
    int playcount = -1;

public:
    VideoDecoder(JavaJNICallback *javaCall, AudioDecoder *audio, PlayStatus *playStatus);
    ~VideoDecoder();

    void playVideo(int codecType);
    void decodVideo();
    void release();
    double synchronize(AVFrame *srcFrame, double pts);

    double getDelayTime(double diff);

    void setClock(int secds);

};


#endif //WLPLAYER_WLVIDEO_H
