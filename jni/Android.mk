LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := watchdoge
LOCAL_SRC_FILES := main.cpp wd/send.cpp wd/device.cpp wd/sniffer.cpp
LOCAL_CPPFLAGS := -std=c++11 -fdiagnostics-color=always -fexceptions -frtti #-O0 -ggdb -fno-omit-frame-pointer -funwind-tables
LOCAL_LDLIBS := -llog

LOCAL_C_INCLUDES += jni/vendor/pcap
LOCAL_C_INCLUDES += jni/vendor/msgpack/include
LOCAL_C_INCLUDES += jni/vendor/queue
LOCAL_C_INCLUDES += jni/vendor/optional

LOCAL_STATIC_LIBRARIES := libpcap

include $(BUILD_EXECUTABLE)
include jni/vendor/pcap/Android.mk
