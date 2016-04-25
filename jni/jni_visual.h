#include <jni.h>

extern "C" {

int g_thresh = 120;
#define TAG "visual_jni"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

JNIEXPORT void JNICALL Java_ct_codec_dji_sdk_basicopencv_TrackingArea(JNIEnv*, jobject, jint startX, jint startY, jint width, jint height);

JNIEXPORT jfloatArray JNICALL Java_ct_codec_dji_sdk_basicopencv_Track(JNIEnv *env, jobject, jlong addrSource, jlong addrGray, jlong addrCanny);


}
