# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

add_library(libcommontool STATIC  thread.cpp
                PlayerQueue.cpp
            	message_queue/handler.cpp
            	message_queue/message_queue.cpp
            	opensl_media/opensl_es_context.cpp
            	 )