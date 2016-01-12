LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := watchdoge
LOCAL_SRC_FILES := main.cpp
LOCAL_CPPFLAGS := -std=c++11 -fexceptions
LOCAL_LDLIBS := -llog

LOCAL_C_INCLUDES += jni/vendor/pcap
LOCAL_C_INCLUDES += jni/vendor/msgpack/include

LOCAL_STATIC_LIBRARIES := libpcap

include $(BUILD_EXECUTABLE)
include jni/vendor/pcap/Android.mk
