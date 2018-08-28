#include "./audio_output.h"

#define LOG_TAG "AudioOutput"

AudioOutput::AudioOutput() {
}

AudioOutput::~AudioOutput() {
}

SLresult AudioOutput::registerPlayerCallback() {
    // Register the player callback
    return (*audioPlayerBufferQueue)->RegisterCallback(audioPlayerBufferQueue,
                                                       this->produceDataCallback,
                                                       this->ctx); // player context
}

//void AudioOutput::playerCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
//	AudioOutput* audioOutput = (AudioOutput*) context;
//	audioOutput->producePacket();
//}



//void AudioOutput::producePacket() {
//	//回调playerController中的方法来获得buffer
//	if (playingState == PLAYING_STATE_PLAYING && produceDataCallback != NULL) {
//        LOGD("producePacket************** =1");
//		int actualSize = produceDataCallback(buffer, bufferSize, ctx);
//        LOGD("producePacket************** = %d", actualSize);
//		if (actualSize > 0 && playingState == PLAYING_STATE_PLAYING) {
//			//将提供的数据加入到播放的buffer中去
//			(*audioPlayerBufferQueue)->Enqueue(audioPlayerBufferQueue, buffer, actualSize);
//		}
//	}
//}

SLresult AudioOutput::pause() {
    return setAudioPlayerStatePaused();
}

SLresult AudioOutput::stop() {
    LOGI("Set the audio player state paused");
    // Set the audio player state playing
    SLresult result = setAudioPlayerStatePaused();
    if (SL_RESULT_SUCCESS != result) {
        return result;
    }
    playingState = PLAYING_STATE_STOPPED;
    usleep(0.05 * 1000000);
    LOGI("destroyContext...");
    destroyContext();
    return SL_RESULT_SUCCESS;
}

SLresult AudioOutput::play() {
//	LOGI("Set the audio player state playing");
    // Set the audio player state playing
    SLresult result = setAudioPlayerStatePlaying();
    if (SL_RESULT_SUCCESS != result) {
        return result;
    }
    playingState = PLAYING_STATE_PLAYING;
    return SL_RESULT_SUCCESS;
}

SLresult AudioOutput::start() {
//	LOGI("Set the audio player state start");
    // Set the audio player state playing
    SLresult result = setAudioPlayerStatePlaying();
    if (SL_RESULT_SUCCESS != result) {
        return result;
    }
    playingState = PLAYING_STATE_PLAYING;
//	LOGI(" Enqueue the first buffer to start");
    // Enqueue the first buffer to start
//	producePacket();

    return SL_RESULT_SUCCESS;
}

SLresult AudioOutput::initSoundTrack(int channels, int accompanySampleRate,
                                     pcmBufferCallBack audioPlayerCallback1, void *ctx) {
    LOGI("enter AudioOutput::initSoundTrack");

//	LOGI("get open sl es Engine");
    this->ctx = ctx;
    this->produceDataCallback = audioPlayerCallback1;
    SLresult result = SL_RESULT_UNKNOWN_ERROR;
    OpenSLESContext *openSLESContext = OpenSLESContext::GetInstance();
    engineEngine = openSLESContext->getEngine();

    if (engineEngine == NULL) {
        return result;
    }

    LOGI("Create output mix object");
    // Create output mix object
    result = createOutputMix();
    if (SL_RESULT_SUCCESS != result) {
        return result;
    }

    LOGI("Realize output mix object");
    // Realize output mix object
    result = realizeObject(outputMixObject);
    if (SL_RESULT_SUCCESS != result) {
        return result;
    }

    LOGI("Initialize buffer");
    //Calculate the buffer size default 50ms length
    bufferSize = channels * accompanySampleRate * 2 * 2 / 3;

    // Initialize buffer
    initPlayerBuffer();

    LOGI("Create the buffer queue audio player object");
    // Create the buffer queue audio player object
    result = createAudioPlayer(channels, accompanySampleRate);
    if (SL_RESULT_SUCCESS != result) {
        freePlayerBuffer();
        destroyObject(outputMixObject);
        return result;
    }

    LOGI("Realize audio player object");
    // Realize audio player object
    result = realizeObject(audioPlayerObject);
    if (SL_RESULT_SUCCESS != result) {
        freePlayerBuffer();
        destroyObject(outputMixObject);
        return result;
    }

    LOGI("Get audio player buffer queue interface");
    // Get audio player buffer queue interface
    result = getAudioPlayerBufferQueueInterface();
    if (SL_RESULT_SUCCESS != result) {
        destroyObject(audioPlayerObject);
        freePlayerBuffer();
        destroyObject(outputMixObject);
        return result;
    }

    LOGI("Registers the player callback");

    result = (*audioPlayerBufferQueue)->RegisterCallback(audioPlayerBufferQueue,
                                                         produceDataCallback,
                                                         this->ctx); // player context
    if (SL_RESULT_SUCCESS != result) {
        destroyObject(audioPlayerObject);
        freePlayerBuffer();
        destroyObject(outputMixObject);
        return result;
    }

    LOGI("Get audio player play interface");
    // Get audio player play interface
    result = getAudioPlayerPlayInterface();
    if (SL_RESULT_SUCCESS != result) {
        destroyObject(audioPlayerObject);
        freePlayerBuffer();
        destroyObject(outputMixObject);
        return result;
    }

    getAudioPlayerVolumInterface();

    getAudioPlayerMuteInterface();

    LOGI("leave init");


    return SL_RESULT_SUCCESS;
}

long AudioOutput::getCurrentTimeMills() {
    SLmillisecond position = 0;
    if (0 != audioPlayerObject && NULL != (*audioPlayerPlay)) {
        SLresult result = (*audioPlayerPlay)->GetPosition(audioPlayerPlay, &position);
    }
    return position;
}

bool AudioOutput::isPlaying() {
    bool result = false;
    SLuint32 pState = SL_PLAYSTATE_PLAYING;
    if (0 != audioPlayerObject && NULL != (*audioPlayerPlay)) {
        SLresult result = (*audioPlayerPlay)->GetPlayState(audioPlayerPlay, &pState);
    } else {
        result = false;
    }
    if (pState == SL_PLAYSTATE_PLAYING) {
        result = true;
    }
    return result;
}

void AudioOutput::destroyContext() {
    LOGI("enter AudioOutput::DestroyContext");
    // Destroy audio player object
    destroyObject(audioPlayerObject);
    LOGI("after destroy audioPlayerObject");
    // Free the player buffer
    freePlayerBuffer();
    LOGI("after FreePlayerBuffer");
    // Destroy output mix object
    destroyObject(outputMixObject);

    pcmMutePlay = NULL;
    pcmVolumePlay = NULL;

    LOGI("leave AudioOutput::DestroyContext");
}

/** 以下是私有方法的实现 **/
SLresult AudioOutput::realizeObject(SLObjectItf object) {
    return (*object)->Realize(object, SL_BOOLEAN_FALSE); // No async, blocking call
}

void AudioOutput::destroyObject(SLObjectItf &object) {
    if (0 != object)
        (*object)->Destroy(object);
    object = 0;
}

SLresult AudioOutput::createOutputMix() {
    // Create output mix object
    return (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, // no interfaces
                                            0, // no interfaces
                                            0); // no required
}

void AudioOutput::initPlayerBuffer() {
    //Initialize buffer
    buffer = new byte[bufferSize];
}

void AudioOutput::freePlayerBuffer() {
    if (NULL != buffer) {
        delete[] buffer;
        buffer = NULL;
    }
}

SLresult AudioOutput::createAudioPlayer(int channels, int accompanySampleRate) {
    SLDataLocator_AndroidSimpleBufferQueue dataSourceLocator = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, // locator type
            1 // buffer count
    };
    int samplesPerSec = opensl_get_sample_rate(accompanySampleRate);
    int channelMask = getChannelMask(channels);
    SLDataFormat_PCM dataSourceFormat = {SL_DATAFORMAT_PCM, // format type
                                         (SLuint32) channels, // channel count
                                         (SLuint32) samplesPerSec, // samples per second in millihertz
                                         SL_PCMSAMPLEFORMAT_FIXED_16, // bits per sample
                                         SL_PCMSAMPLEFORMAT_FIXED_16, // container size
                                         (SLuint32) channelMask, // channel mask
                                         SL_BYTEORDER_LITTLEENDIAN // endianness
    };

    // Data source is a simple buffer queue with PCM format
    SLDataSource dataSource = {&dataSourceLocator, // data locator
                               &dataSourceFormat // data format
    };

    // Output mix locator for data sink
    SLDataLocator_OutputMix dataSinkLocator = {SL_DATALOCATOR_OUTPUTMIX, // locator type
                                               outputMixObject // output mix
    };

    // Data sink is an output mix
    SLDataSink dataSink = {&dataSinkLocator, // locator
                           0 // format
    };

    // Interfaces that are requested
    SLInterfaceID interfaceIds[] = {SL_IID_BUFFERQUEUE , SL_IID_VOLUME, SL_IID_PLAYBACKRATE,
                                    SL_IID_MUTESOLO};

    // Required interfaces. If the required interfaces
    // are not available the request will fail
    SLboolean requiredInterfaces[] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};


    // Create audio player object
    return (*engineEngine)->CreateAudioPlayer(engineEngine, &audioPlayerObject, &dataSource,
                                              &dataSink, ARRAY_LEN(interfaceIds), interfaceIds,
                                              requiredInterfaces);
}

SLresult AudioOutput::getAudioPlayerBufferQueueInterface() {
    return (*audioPlayerObject)->GetInterface(audioPlayerObject, SL_IID_BUFFERQUEUE,
                                              &audioPlayerBufferQueue);
}

SLresult AudioOutput::getAudioPlayerPlayInterface() {
    return (*audioPlayerObject)->GetInterface(audioPlayerObject, SL_IID_PLAY, &audioPlayerPlay);
}

SLresult AudioOutput::setAudioPlayerStatePlaying() {
    SLresult result = (*audioPlayerPlay)->SetPlayState(audioPlayerPlay, SL_PLAYSTATE_PLAYING);
    return result;
}

SLresult AudioOutput::setAudioPlayerStatePaused() {
    SLresult result = (*audioPlayerPlay)->SetPlayState(audioPlayerPlay, SL_PLAYSTATE_PAUSED);
    return result;
}

SLAndroidSimpleBufferQueueItf AudioOutput::getSLQueueItf() {
    return audioPlayerBufferQueue;
}

SLresult AudioOutput::getAudioPlayerVolumInterface() {
    SLresult lresult = (*audioPlayerObject)->GetInterface(audioPlayerObject, SL_IID_VOLUME,
                                                          &pcmVolumePlay);
    return lresult;
}

SLresult AudioOutput::getAudioPlayerMuteInterface() {
    SLresult lresult = (*audioPlayerObject)->GetInterface(audioPlayerObject, SL_IID_MUTESOLO,
                                                          &pcmMutePlay);
    return lresult;
}

SLresult AudioOutput::setVolume(int percent) {
    volumePercent = percent;
    SLresult lresult = 0;
    LOGI("setVolume percent = %d pcmVolumePlay = %p", 0, pcmVolumePlay);
    if (pcmVolumePlay != NULL) {
        if (percent > 30) {
            lresult = (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -20);
        } else if (percent > 25) {
            lresult = (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -22);
        } else if (percent > 20) {
            lresult = (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -25);
        } else if (percent > 15) {
            lresult = (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -28);
        } else if (percent > 10) {
            lresult = (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -30);
        } else if (percent > 5) {
            lresult = (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -34);
        } else if (percent > 3) {
            lresult = (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -37);
        } else if (percent > 0) {
            lresult = (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -40);
        } else {
            lresult = lresult = (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay,
                                                                 (100 - percent) * -100);
        }
    }

    return lresult;
}

SLresult AudioOutput::setVolMute(SLboolean mute) {
    volMute = mute;
    if(pcmVolumePlay != NULL) {
        return (*pcmVolumePlay)->SetMute(pcmVolumePlay, mute);
    }

    return 0;
}

SLresult AudioOutput::setChannelMute(int mute) {
    this->mute = mute;
    if (pcmMutePlay != NULL) {
        if (mute == 0)//right
        {
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, false);
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, true);
        } else if (mute == 1)//left
        {
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, true);
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, false);
        } else if (mute == 2)//center
        {
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, false);
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, false);
        }


    }

    return 0;
}


