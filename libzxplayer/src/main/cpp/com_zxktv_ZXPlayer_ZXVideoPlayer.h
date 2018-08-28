/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_zxktv_ZXPlayer_ZXVideoPlayer */

#ifndef _Included_com_zxktv_ZXPlayer_ZXVideoPlayer
#define _Included_com_zxktv_ZXPlayer_ZXVideoPlayer
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_zxktv_ZXPlayer_ZXVideoPlayer
 * Method:    nativePrepared
 * Signature: (Ljava/lang/String;Z)V
 */
JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativePrepared
  (JNIEnv *, jobject, jstring, jboolean);

/*
 * Class:     com_zxktv_ZXPlayer_ZXVideoPlayer
 * Method:    nativeStart
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeStart
  (JNIEnv *, jobject);

/*
 * Class:     com_zxktv_ZXPlayer_ZXVideoPlayer
 * Method:    nativeStop
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeStop
  (JNIEnv *, jobject, jboolean);

/*
 * Class:     com_zxktv_ZXPlayer_ZXVideoPlayer
 * Method:    nativePause
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativePause
  (JNIEnv *, jobject);

/*
 * Class:     com_zxktv_ZXPlayer_ZXVideoPlayer
 * Method:    nativeResume
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeResume
  (JNIEnv *, jobject);

/*
 * Class:     com_zxktv_ZXPlayer_ZXVideoPlayer
 * Method:    nativeSeek
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSeek
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_zxktv_ZXPlayer_ZXVideoPlayer
 * Method:    nativeSetAudioChannels
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSetAudioChannels
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_zxktv_ZXPlayer_ZXVideoPlayer
 * Method:    nativeGetDuration
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeGetDuration
  (JNIEnv *, jobject);

/*
 * Class:     com_zxktv_ZXPlayer_ZXVideoPlayer
 * Method:    nativeGetAudioChannels
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeGetAudioChannels
  (JNIEnv *, jobject);

JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSetVolume(JNIEnv *, jobject, jint);

JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSetVolMute
        (JNIEnv *, jobject, jboolean);

JNIEXPORT void JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeSetChannelMute
        (JNIEnv *, jobject, jint);

/*
 * Class:     com_zxktv_ZXPlayer_ZXVideoPlayer
 * Method:    nativeGetVideoWidth
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeGetVideoWidth
  (JNIEnv *, jobject);

/*
 * Class:     com_zxktv_ZXPlayer_ZXVideoPlayer
 * Method:    nativeGetVideoHeight
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_zxktv_ZXPlayer_ZXVideoPlayer_nativeGetVideoHeight
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
