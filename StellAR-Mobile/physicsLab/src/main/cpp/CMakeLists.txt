cmake_minimum_required(VERSION 3.22)

set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 17)

# Must be before add_subdirectory(raylib)
set(PLATFORM "Android" CACHE STRING "" FORCE)

project("${APP_LIB_NAME}")

add_subdirectory(${CMAKE_SOURCE_DIR}/deps/raylib)
add_subdirectory(${CMAKE_SOURCE_DIR}/deps/raymob)

set(SOURCES
    main.cpp
    Physics.cpp
    JNIBridge.cpp
    Collision.cpp
    HUD.cpp
    Input.cpp
)

include_directories(${ANDROID_NDK}/sources/android/native_app_glue/)
list(APPEND SOURCES ${ANDROID_NDK}/sources/android/native_app_glue/android_native_app_glue.c)

add_library(${APP_LIB_NAME} SHARED ${SOURCES})

target_compile_definitions(${APP_LIB_NAME} PRIVATE PLATFORM_ANDROID)

if(CMAKE_BUILD_TYPE MATCHES "Debug")
    target_compile_definitions(${APP_LIB_NAME} PRIVATE _DEBUG DEBUG)
endif()

target_include_directories(${APP_LIB_NAME} PRIVATE
    "${CMAKE_SOURCE_DIR}/deps/raylib/src"
    "${CMAKE_SOURCE_DIR}/deps/raymob"
)

target_link_libraries(${APP_LIB_NAME}
    raylib
    raymoblib
    c++_shared
    log
    android
    EGL
    GLESv2
    OpenSLES
    atomic
    m
)
