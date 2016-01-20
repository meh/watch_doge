LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := backend
LOCAL_CPPFLAGS  := -std=gnu++1y -fdiagnostics-color=always -fexceptions -frtti
LOCAL_LDLIBS    := -llog

LOCAL_SRC_FILES := main.cpp send.cpp device.cpp sniffer.cpp \
	packet/unknown.cpp packet/ethernet.cpp packet/ip.cpp

LOCAL_C_INCLUDES += jni/vendor/pcap
LOCAL_C_INCLUDES += jni/vendor/msgpack/include
LOCAL_C_INCLUDES += jni/vendor/queue
LOCAL_C_INCLUDES += jni/vendor/optional

LOCAL_STATIC_LIBRARIES := libpcap

include $(BUILD_EXECUTABLE)
include jni/vendor/pcap/Android.mk
