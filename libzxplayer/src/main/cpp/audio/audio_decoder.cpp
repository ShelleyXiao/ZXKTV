#include <opensl_media/opensl_es_context.h>
#include "audio_decoder.h"

#define LOG_TAG "AUDIO"

AudioDecoder::AudioDecoder(WlPlayStatus
                           *playStatus,
                           JavaJNICallback *javaJNICall
) {
    streamIndex = -1;
    out_buffer = (uint8_t *) malloc(sample_rate * 2 * 2 * 3 / 2);
    queue = new WlQueue(playStatus);
    wlPlayStatus = playStatus;
    this->javaJNICall = javaJNICall;
    dst_format = AV_SAMPLE_FMT_S16;

    buffSize = sample_rate * channels * av_get_bytes_per_sample(dst_format);

    sampleBuffer = static_cast<SAMPLETYPE *>(malloc(
            sample_rate * channels * av_get_bytes_per_sample(dst_format)));
    processBuffer = static_cast<SAMPLETYPE *>(malloc(
            sample_rate * channels * av_get_bytes_per_sample(dst_format)));
    soundTouch = new SoundTouch();
    soundTouch->setSampleRate(sample_rate);
    soundTouch->setChannels(2);


    soundTouch->setPitch(0.8f);
//    soundTouch->setPitchOctaves(-0.5);
//    soundTouch->setPitchSemiTones(8);
    soundTouch->setTempo(this->speed);
    soundTouch->setRate(1.0f);
}

AudioDecoder::~AudioDecoder() {
    if (LOG_SHOW) {
        LOGE("audio_decoderecoder() 释放完了");
    }
}

void AudioDecoder::realease() {
    if (LOG_SHOW) {
        LOGE("开始释放 audio...");
    }
    pause();
    if (queue != NULL) {
        queue->noticeThread();
    }
    int count = 0;
    while (!isExit) {
        if (LOG_SHOW) {
            LOGD("等待缓冲线程结束...%d", count);
        }
        if (count > 1000) {
            isExit = true;
        }
        count++;
        av_usleep(1000 * 10);
    }
    if (queue != NULL) {
        queue->release();
        delete (queue);
        queue = NULL;
    }

    LOG_DEBUG("释放 opensl es start");


    if (audioOutput != NULL) {
        audioOutput->destroyContext();
    }

    if (LOG_SHOW) {
        LOGE("释放 opensl es end");
    }


    if (out_buffer != NULL) {
        free(out_buffer);
        out_buffer = NULL;
    }
//    if (buffer != NULL) {
//        free(buffer);
//        buffer = NULL;
//    }
    if (avCodecContext != NULL) {
        avcodec_close(avCodecContext);
        avcodec_free_context(&avCodecContext);
        avCodecContext = NULL;
    }
    if (wlPlayStatus != NULL) {
        wlPlayStatus = NULL;
    }

}

void *audioPlayThread(void *context) {
    AudioDecoder *audio = (AudioDecoder *) context;
    audio->initAudioOutput();
    pthread_exit(&audio->audioThread);
}

void AudioDecoder::playAudio() {
    pthread_create(&audioThread, NULL, audioPlayThread, this);
}

int AudioDecoder::getPcmData(void **pcm) {
    while (!wlPlayStatus->exit) {
        isExit = false;

        if (wlPlayStatus->pause)//暂停
        {
            av_usleep(1000 * 100);
            continue;
        }
        if (wlPlayStatus->seek) {
            javaJNICall->onLoad(WL_THREAD_CHILD, true);
            wlPlayStatus->load = true;
            isReadPacketFinish = true;
            continue;
        }
        if (!isVideo) {
            if (queue->getAvPacketSize() == 0)//加载
            {
                if (!wlPlayStatus->load) {
                    javaJNICall->onLoad(WL_THREAD_CHILD, true);
                    wlPlayStatus->load = true;
                }
                continue;
            } else {
                if (wlPlayStatus->load) {
                    javaJNICall->onLoad(WL_THREAD_CHILD, false);
                    wlPlayStatus->load = false;
                }
            }
        }
        if (isReadPacketFinish) {
            isReadPacketFinish = false;
            packet = av_packet_alloc();
            if (this->queue->getAvpacket(packet) != 0) {
                av_packet_free(&packet);
                av_free(packet);
                packet = NULL;
                isReadPacketFinish = true;
                continue;
            }
            ret = avcodec_send_packet(avCodecContext, packet);
            if (ret < 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF) {
                av_packet_free(&packet);
                av_free(packet);
                packet = NULL;
                isReadPacketFinish = true;
                continue;
            }
        }
//        LOGD("22222222222222222222222222222222222222");
        AVFrame *frame = av_frame_alloc();
        if (avcodec_receive_frame(avCodecContext, frame) == 0) {
            // 设置通道数或channel_layout
            if (frame->channels > 0 && frame->channel_layout == 0) {
                frame->channel_layout = av_get_default_channel_layout(frame->channels);
            } else if (frame->channels == 0 && frame->channel_layout > 0) {
                frame->channels = av_get_channel_layout_nb_channels(frame->channel_layout);
            }
            SwrContext *swr_ctx;
            //重采样为立体声
            dst_layout = AV_CH_LAYOUT_STEREO;
            // 设置转换参数
            swr_ctx = swr_alloc_set_opts(NULL, dst_layout, dst_format, frame->sample_rate,
                                         frame->channel_layout,
                                         (enum AVSampleFormat) frame->format,
                                         frame->sample_rate, 0, NULL);
            if (!swr_ctx || (ret = swr_init(swr_ctx)) < 0) {
                av_frame_free(&frame);
                av_free(frame);
                frame = NULL;
                swr_free(&swr_ctx);
                av_packet_free(&packet);
                av_free(packet);
                packet = NULL;
                continue;
            }

            // 计算转换后的sample个数 a * b / c
            dst_nb_samples = (int) av_rescale_rnd(
                    swr_get_delay(swr_ctx, frame->sample_rate) + frame->nb_samples,
                    frame->sample_rate, frame->sample_rate, AV_ROUND_INF);
//            LOGD("22222244444444446666666666622222");
            // 转换，返回值为转换后的sample个数
            nb = swr_convert(swr_ctx, &out_buffer, dst_nb_samples,
                             (const uint8_t **) frame->data, frame->nb_samples);
            //根据布局获取声道数
            out_channels = av_get_channel_layout_nb_channels(dst_layout);
            data_size = out_channels * nb * av_get_bytes_per_sample(dst_format);
            now_time = frame->pts * av_q2d(time_base);
            if (now_time < clock) {
                now_time = clock;
            }
            clock = now_time;
            av_frame_free(&frame);
            av_free(frame);
            frame = NULL;
            swr_free(&swr_ctx);
            *pcm = out_buffer;
            break;
        } else {
            isReadPacketFinish = true;
            av_frame_free(&frame);
            av_free(frame);
            frame = NULL;
            av_packet_free(&packet);
            av_free(packet);
            packet = NULL;
            continue;
        }
    }
    isExit = true;
    return data_size;
}

//void pcmBufferCallBack_sl(SLAndroidSimpleBufferQueueItf bf, void *context) {
//    AudioDecoder *audioDecoer = (AudioDecoder *) context;
//    if (audioDecoer != NULL) {
//        if (LOG_SHOW) {
//            LOGE("pcm call back...");
//        }
//        audioDecoer->buffer = NULL;
//        audioDecoer->pcmsize = audioDecoer->getPcmData(&audioDecoer->buffer);
//        if (audioDecoer->buffer && audioDecoer->pcmsize > 0) {
//            audioDecoer->clock +=
//                    audioDecoer->pcmsize / ((double) (audioDecoer->sample_rate * 2 * 2));
//            audioDecoer->javaJNICall->onVideoInfo(WL_THREAD_CHILD, audioDecoer->clock,
//                                                  audioDecoer->duration);
////            (*wlAudio->pcmBufferQueue)->Enqueue(wlAudio->pcmBufferQueue, wlAudio->buffer,
////                                                wlAudio->pcmsize);
//            SLAndroidSimpleBufferQueueItf pcmBufferQueue = audioDecoer->audioOutput->getSLQueueItf();
//            if (pcmBufferQueue != NULL) {
//                (*pcmBufferQueue)->Enqueue(pcmBufferQueue,
//                                           audioDecoer->buffer,
//                                           audioDecoer->pcmsize);
//            }
//        }
//    }
//}

void pcmBufferCallBack_sl(SLAndroidSimpleBufferQueueItf bf, void *context) {
    AudioDecoder *audioDecoer = (AudioDecoder *) context;
    if (audioDecoer != NULL) {
//        LOGD("************* DEBUG 1");
        audioDecoer->buffer = NULL;
        audioDecoer->pcmsize = audioDecoer->getPcmData(&audioDecoer->buffer);

        int sampleSize = audioDecoer->getSoundTouchData(audioDecoer->buffer, audioDecoer->pcmsize,
                                                        context);


//        LOGD("************* DEBUG sampleSize = %d", sampleSize);

        if (audioDecoer->processBuffer && sampleSize > 0) {

            audioDecoer->clock +=
                    (double) (sampleSize * audioDecoer->channels *
                              av_get_bytes_per_sample(audioDecoer->dst_format) /
                              ((double) (audioDecoer->sample_rate * audioDecoer->channels *
                                         av_get_bytes_per_sample(audioDecoer->dst_format))));

            audioDecoer->javaJNICall->onVideoInfo(WL_THREAD_CHILD, audioDecoer->clock,
                                                  audioDecoer->duration);
            SLAndroidSimpleBufferQueueItf pcmBufferQueue = audioDecoer->audioOutput->getSLQueueItf();
            if (pcmBufferQueue != NULL) {
                (*pcmBufferQueue)->Enqueue((pcmBufferQueue),
                                           audioDecoer->processBuffer,
                                           sampleSize * audioDecoer->channels *
                                           av_get_bytes_per_sample(audioDecoer->dst_format));
            }
        }
    }
}


bool AudioDecoder::initAudioOutput() {
    LOGI("VideoPlayerController::initAudioOutput");
    audioOutput = new AudioOutput();
    SLresult result = audioOutput->initSoundTrack(channels, sample_rate, pcmBufferCallBack_sl,
                                                  this);

    if (SL_RESULT_SUCCESS != result) {
        LOGI("audio manager failed on initialized...");
        delete audioOutput;
        audioOutput = NULL;
        return false;
    }
    audioOutput->start();

    SLAndroidSimpleBufferQueueItf pcmBufferQueue = this->audioOutput->getSLQueueItf();
    if (pcmBufferQueue != NULL) {
        pcmBufferCallBack_sl(pcmBufferQueue, this);
    }

    return true;
}


void AudioDecoder::pause() {
    if (audioOutput != NULL) {
        audioOutput->pause();
    }
}

void AudioDecoder::resume() {
    if (audioOutput != NULL) {
        audioOutput->play();
    }
}

void AudioDecoder::setVideo(bool video) {
    isVideo = video;
}

void AudioDecoder::setClock(int secds) {
    now_time = secds;
    clock = secds;
}

void AudioDecoder::setVolume(int percent) {
    if (audioOutput != NULL) {
        audioOutput->setVolume(percent);
    }
}

void AudioDecoder::setVolMute(bool mute) {
    if (audioOutput != NULL) {
        audioOutput->setVolMute(mute);
    }
}

void AudioDecoder::setChannelMute(int mute) {
    if (audioOutput != NULL) {
        audioOutput->setChannelMute(mute);
    }
}


int AudioDecoder::getSoundTouchData(void *data_in, int data_size, void *context) {
    AudioDecoder *decoder = (AudioDecoder *) context;
    memset(decoder->sampleBuffer, 0, decoder->buffSize);
    memset(decoder->processBuffer, 0, decoder->buffSize);
    int num = 0;
    while (decoder->wlPlayStatus != NULL && !decoder->wlPlayStatus->exit) {
        if (decoder->finished) {
            decoder->finished = false;

            uint8_t *audioBufer = (uint8_t *) data_in;
//            LOGD("*********************** data_size = %d", data_size);
            if (data_size > 0) {
                for (int i = 0; i < data_size / 2 + 1; i++) {
                    decoder->sampleBuffer[i] = (audioBufer[i * 2] | (audioBufer[i * 2 + 1]) << 8);
                }
                decoder->soundTouch->putSamples(decoder->sampleBuffer, decoder->nb);
//
                num = decoder->soundTouch->receiveSamples(decoder->processBuffer, data_size / 4);
            } else {
                decoder->soundTouch->flush();
            }
        }

        if (num == 0) {
            decoder->finished = true;
            memset(decoder->processBuffer, 0, decoder->sample_rate * 2 * 2);
            continue;
        } else {
            if (audioBufer == NULL) {
                num = decoder->soundTouch->receiveSamples(decoder->processBuffer, data_size / 4);
                if (num == 0) {
                    decoder->finished = true;
                    memset(decoder->processBuffer, 0, decoder->sample_rate * 2 * 2);
                    continue;
                }
            }

            return num;
        }
    }
    return 0;
}

//int AudioDecoder::getSoundTouchData(void *data_in, int data_size, void *context) {
//    AudioDecoder *decoder = (AudioDecoder *) context;
//    memset(decoder->sampleBuffer, 0, decoder->sample_rate * channels * av_get_bytes_per_sample(dst_format));
//
//    SAMPLETYPE pcmBufer[decoder->sample_rate * channels * av_get_bytes_per_sample(dst_format)];
//
//    int nb = 0;
//    int pcmDataSize = 0, totalPcmSize = 0;
//    while (decoder->wlPlayStatus != NULL && !decoder->wlPlayStatus->exit) {
//        if (data_size > 0) {
//            uint8_t *audioBufer = (uint8_t *) data_in;
//            for (int i = 0; i < data_size / 2 + 1; i++) {
//                decoder->sampleBuffer[i] = (audioBufer[i * 2] | (audioBufer[i * 2 + 1]) << 8);
//            }
//            decoder->soundTouch->putSamples(decoder->sampleBuffer, decoder->nb);
//            do {
//                nb = decoder->soundTouch->receiveSamples(pcmBufer, sample_rate / channels);
//                pcmDataSize += nb * channels * av_get_bytes_per_sample(dst_format);
//            } while (nb != 0);
//            memcpy(decoder->sampleBuffer, pcmBufer, pcmDataSize);
//
//            memset(pcmBufer, 0,
//                   decoder->sample_rate * channels * av_get_bytes_per_sample(dst_format));
//            decoder->soundTouch->flush();
//            totalPcmSize += pcmDataSize;
//            pcmDataSize = 0;
//
//            do {
//                nb = decoder->soundTouch->receiveSamples(pcmBufer, sample_rate / channels);
//                pcmDataSize += nb * channels * av_get_bytes_per_sample(dst_format);
//            } while (nb != 0);
//            memcpy(decoder->sampleBuffer + totalPcmSize, pcmBufer, pcmDataSize);
//            decoder->soundTouch->flush();
//            totalPcmSize += pcmDataSize;
//
//        }
//
//        return totalPcmSize;
//    }
//    return 0;
//}


void AudioDecoder::setPitch(float pitch) {
    this->pitch = pitch;
    if (soundTouch != NULL) {
        soundTouch->setPitch(pitch);
        LOGD("setPitch pitch = %f", pitch);
        soundTouch->setTempo(1.0f);
    }
}

float AudioDecoder::getPitch() {
    return this->pitch;
}

void AudioDecoder::setSpeed(float speed) {
    this->speed = speed;
    if (soundTouch != NULL) {
        soundTouch->setTempo(speed);
    }
}





