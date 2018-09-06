#include <jni.h>
#include <stddef.h>
#include <libaudio_effect/audio_effect/audio_effect_adapter.h>
#include "common/AndroidLog.h"
#include "JavaJNICallback.h"
#include "video_player_control.h"
#include "com_zxktv_ZXPlayer_ZXVideoPlayer.h"

#define LOG_TAG "jni"

_JavaVM *javaVM = NULL;
JavaJNICallback *javaJNICallback = NULL;
VideoPlayerControl *playerControl = NULL;


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    jint result = -1;
    javaVM = vm;
    JNIEnv *env;

    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        if (LOG_SHOW) {
            LOGE("GetEnv failed!");
        }
        return result;
    }
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativePrepared(JNIEnv *env, jobject instance, jstring url_,
                                                     jboolean isOnlyMusic) {
    const char *url = env->GetStringUTFChars(url_, 0);

    if (javaJNICallback == NULL) {
        javaJNICallback = new JavaJNICallback(javaVM, env, &instance);
    }
    if (playerControl == NULL) {
        playerControl = new VideoPlayerControl(javaJNICallback, url, isOnlyMusic);
        javaJNICallback->onLoad(WL_THREAD_MAIN, true);
        playerControl->preparedFFmpeg();
    }
}


JNIEXPORT void JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeStart(JNIEnv *env, jobject instance) {
    if (playerControl != NULL) {
        playerControl->start();
    }

}

JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeStop
        (JNIEnv *env, jobject instance, jboolean exit) {
    if (playerControl != NULL) {
        playerControl->exitByUser = true;
        playerControl->release();
        delete (playerControl);
        playerControl = NULL;

        if (javaJNICallback != NULL) {
            javaJNICallback->release();
            javaJNICallback = NULL;
        }
        if (!exit) {
            jclass jlz = env->GetObjectClass(instance);
            jmethodID jmid_stop = env->GetMethodID(jlz, "onStopComplete", "()V");
            env->CallVoidMethod(instance, jmid_stop);
        }
    }
}


JNIEXPORT void JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativePause(JNIEnv *env, jobject instance) {
    if (playerControl != NULL) {
        playerControl->pause();
    }
}

JNIEXPORT void JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeResume(JNIEnv *env, jobject instance) {
    if (playerControl != NULL) {
        playerControl->resume();
    }

}

JNIEXPORT void JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSeek(JNIEnv *env, jobject instance, jint secds) {
    if (playerControl != NULL) {
        playerControl->seek(secds);
    }

}

JNIEXPORT jint JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeGetDuration(JNIEnv *env, jobject instance) {
    if (playerControl != NULL) {
        return playerControl->getDuration();
    }
    return 0;

}

JNIEXPORT void JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSetAudioChannels(JNIEnv *env, jobject instance,
                                                             jint index) {
    if (playerControl != NULL) {
        playerControl->setAudioChannel(index);
    }
}

JNIEXPORT jint JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeGetAudioChannels(JNIEnv *env, jobject instance) {

    if (playerControl != NULL) {
        return playerControl->getAudioChannels();
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSetVolume(JNIEnv *env, jobject instance, jint percent) {
    if (playerControl != NULL) {
        playerControl->setVolume(percent);
    }
}

JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSetVolMute
        (JNIEnv *env, jobject instance, jboolean mute) {
    if (playerControl != NULL) {
        playerControl->setVolMute(mute);
    }
}

JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSetChannelMute
        (JNIEnv *env, jobject instance, jint mute) {
    if (playerControl != NULL) {
        playerControl->setChannelMute(mute);
    }
}

JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSetPitch
        (JNIEnv *env, jobject instance, jfloat picth) {
    if (playerControl != NULL) {
        playerControl->setPitch(picth);
    }
}

JNIEXPORT jfloat JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeGetPitch(JNIEnv *env, jobject instance) {
    if (playerControl != NULL) {
        return playerControl->getPitch();
    }
    return 1.0f;
}

JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSetSpeed
        (JNIEnv *env, jobject instance, jfloat speed) {
    if (playerControl != NULL) {
        playerControl->setSpeed(speed);
    }
}

JNIEXPORT void JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSetAudioEffect(JNIEnv *env, jobject obj,
                                                           jobject audioEffectParam) {
    if (playerControl != NULL) {
        AudioEffect *cbAudioEffect = AudioEffectAdapter::GetInstance()->buildAudioEffect(
                audioEffectParam, env);
        playerControl->setAudioEffect(cbAudioEffect);
    }
}

JNIEXPORT jint JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeGetVideoWidth(JNIEnv *env, jobject instance) {
    if (playerControl != NULL) {
        playerControl->getVideoWidth();
    }

}

JNIEXPORT jint JNICALL
Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeGetVideoHeight(JNIEnv *env, jobject instance) {

    if (playerControl != NULL) {
        playerControl->getVideoHeight();
    }

}


