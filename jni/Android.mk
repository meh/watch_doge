LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := backend
LOCAL_LDLIBS := -llog

LOCAL_SRC_FILES := main.cpp send.cpp receiver.cpp device.cpp socket.cpp \
	sniffer/sniffer.cpp sniffer/module.cpp \
	pinger/module.cpp pinger/pinger.cpp \
	tracer/module.cpp tracer/tracer.cpp tracer/historic.cpp tracer/icmp.cpp tracer/tcp.cpp tracer/udp.cpp \

LOCAL_C_INCLUDES += $(LOCAL_PATH)/vendor/pcap
LOCAL_C_INCLUDES += $(LOCAL_PATH)/vendor/msgpack/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/vendor/queue
LOCAL_C_INCLUDES += $(LOCAL_PATH)/vendor/optional
LOCAL_C_INCLUDES += $(LOCAL_PATH)/vendor/variant
LOCAL_C_INCLUDES += $(LOCAL_PATH)/vendor/paku/include

LOCAL_STATIC_LIBRARIES += libpaku
LOCAL_STATIC_LIBRARIES += libpcap

include $(BUILD_EXECUTABLE)
include jni/vendor/pcap/Android.mk
include jni/vendor/paku/Android.mk
