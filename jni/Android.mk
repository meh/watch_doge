LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := backend
LOCAL_LDLIBS := -llog

LOCAL_SRC_FILES := main.cpp send.cpp receiver.cpp device.cpp socket.cpp \
	sniffer/sniffer.cpp sniffer/module.cpp \
	pinger/module.cpp pinger/pinger.cpp \
	packet/packet.cpp packet/unknown.cpp packet/ether.cpp packet/arp.cpp packet/ip.cpp packet/icmp.cpp packet/tcp.cpp \
	packet/builder/buffer.cpp packet/builder/base.cpp packet/builder/icmp.cpp

LOCAL_C_INCLUDES += jni/vendor/pcap
LOCAL_C_INCLUDES += jni/vendor/msgpack/include
LOCAL_C_INCLUDES += jni/vendor/queue
LOCAL_C_INCLUDES += jni/vendor/optional
LOCAL_C_INCLUDES += jni/vendor/variant

LOCAL_STATIC_LIBRARIES += libpcap

include $(BUILD_EXECUTABLE)
include jni/vendor/pcap/Android.mk
