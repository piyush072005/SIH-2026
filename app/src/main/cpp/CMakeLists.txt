cmake_minimum_required(VERSION 3.22.1)
project(itantra_sherpa)

add_library(itantra_sherpa SHARED sherpa_bridge.cpp)

find_library(log-lib log)
target_link_libraries(itantra_sherpa ${log-lib})

# Optional: if SHERPA_ONNX_DIR points at a prebuilt sherpa-onnx Android tree, link it here.
if(DEFINED ENV{SHERPA_ONNX_DIR})
    message(STATUS "SHERPA_ONNX_DIR=$ENV{SHERPA_ONNX_DIR}")
endif()
