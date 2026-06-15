LOCAL_PATH := $(call my-dir)

# Compilação da libItsLoader.so
include $(CLEAR_VARS)
LOCAL_MODULE    := ItsLoader
LOCAL_SRC_FILES := ItsLoader.cpp
LOCAL_LDLIBS    := -llog -landroid
include $(BUILD_SHARED_LIBRARY)

# Compilação da libItsLGL.so
include $(CLEAR_VARS)
LOCAL_MODULE    := ItsLGL
LOCAL_SRC_FILES := ItsLGL.cpp
LOCAL_LDLIBS    := -llog -landroid
include $(BUILD_SHARED_LIBRARY)
