cmake_minimum_required(VERSION 3.17)
project(TeradataCptJni)

#set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 14)
set(PROJECT_INCLUDE ${CMAKE_SOURCE_DIR}/include)
set(JNI_INCLUDE $ENV{JAVA_HOME}/include)
if(WIN32)
    set(JNI_MD_INCLUDE $ENV{JAVA_HOME}/include/win32)
elseif(UNIX AND NOT APPLE)
    set(JNI_MD_INCLUDE $ENV{JAVA_HOME}/include/linux)
endif()

message("PROJECT_INCLUDE:${PROJECT_INCLUDE}")
message("JNI_INCLUDE:${JNI_INCLUDE}")
message("JNI_MD_INCLUDE:${JNI_MD_INCLUDE}")

include_directories(${PROJECT_INCLUDE} ${JNI_INCLUDE} ${JNI_MD_INCLUDE})
add_compile_options(-fPIC -D_REENTRANT)
#add_library(cpt src/cpt.c)
#add_library(TeradataCptJni SHARED src/cptjni.c)
#link_directories(TeradataCptJni cpt)

#add_library(TeradataCptJni SHARED src/cptjni.c src/cpt.o)
add_library(TeradataCptJni SHARED src/cptjni.cpp src/cpt.c src/register.cpp src/convert.cpp)
if(UNIX AND NOT APPLE AND CMAKE_SYSTEM_PROCESSOR MATCHES "^(x86_64|amd64|AMD64)$")
    set_target_properties(TeradataCptJni PROPERTIES OUTPUT_NAME TeradataCptJniAmd64)
elseif(UNIX AND NOT APPLE AND CMAKE_SYSTEM_PROCESSOR MATCHES "^(aarch64|arm64)$")
    set_target_properties(TeradataCptJni PROPERTIES OUTPUT_NAME TeradataCptJniArm64)
endif()

# Test
message("build TeradataCptJniTest")
enable_testing()
set(TEST1 test/multiset_policy_test.c src/cpt.c)
add_executable(multiset_policy_test ${TEST1})
add_test(NAME multiset_policy_test COMMAND multiset_policy_test foxmind_1234_text_139)

# Test2
message("build TeradataCptJniTest2")
set(TEST2 test/simple_single_test.c src/cpt.c)
add_executable(simple_single_test ${TEST2})
add_test(NAME simple_single_test COMMAND simple_single_test foxmind_1234_text_139 中文)

# Binary-safe policy test
set(TEST3 test/binary_policy_test.c src/cpt.c)
add_executable(binary_policy_test ${TEST3})
add_test(NAME binary_policy_test COMMAND binary_policy_test)
