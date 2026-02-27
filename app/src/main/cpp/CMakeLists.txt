# Sets the minimum version of CMake required to build your native library.
cmake_minimum_required(VERSION 3.10)

# Sets the sentencepiece options before adding its subdirectory.
set(SPM_ENABLE_SHARED OFF CACHE BOOL "Builds shared libaries" FORCE)
set(SPM_BUILD_TEST OFF CACHE BOOL "Builds test binaries" FORCE)

# Adds the sentencepiece subdirectory.
add_subdirectory(sentencepiece)

# Ensure sentencepiece-static links to the Android log library.
if(ANDROID)
    target_link_libraries(sentencepiece-static INTERFACE log)
    target_link_libraries(sentencepiece_train-static INTERFACE log)
endif()

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code.
add_library( # Specifies the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             native-lib.cpp)

# Specifies the include directories for the native-lib target.
target_include_directories(native-lib PRIVATE
                           ${CMAKE_CURRENT_SOURCE_DIR}/sentencepiece/src
                           ${CMAKE_CURRENT_SOURCE_DIR}/sentencepiece)

# Links the sentencepiece-static library to the native-lib.
target_link_libraries( # Specifies the target library.
                       native-lib

                       # Links the sentencepiece library to the target library.
                       sentencepiece-static
                       log)
