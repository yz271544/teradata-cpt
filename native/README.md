# 大数据隐私加密

## Build 


### windows 64

```shell
gcc -fPIC -D_REENTRANT -I./include -c ./src/cpt.c -o ./lib/cpt.o
gcc -fPIC -shared -I./include -o ./lib/win64/libTeradataCptJni.dll ./src/cptjni.cpp ./lib/cpt.o ./src/convert.cpp ./src/register.cpp
```
#### 需要设置环境变量
[测试](https://gitee.com/Hu-Lyndon/teradata-cpt.git)
- PATH=${PATH}\;D:\iProject\teradatacpt\lib


### linux
#### tips
需要将对应操作系统的两个文件替换到目录中
- $JAVA_HOME/include/jni.h
- $JAVA_HOME/include/linux/jni_md.h

```shell
# 编译cpt.c为cpt.o
gcc -fPIC -D_REENTRANT -I./include -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -c ./src/cpt.c -o ./lib/cpt.o
# 构建.so文件
gcc -fPIC -shared -I./include -o ./lib/linux-amd64/libTeradataCptJni.so ./src/cptjni.cpp ./lib/cpt.o ./src/convert.cpp ./src/register.cpp
```

#### 需要设置环境变量 .bashrc
[测试](https://gitee.com/Hu-Lyndon/teradata-cpt.git)
- export LD_LIBRARY_PATH='/home/etl/iProject/TeradataCptJni/linux-amd64':${LD_LIBRARY_PATH}
- export PATH=${JAVA_HOME}/bin:${LD_LIBRARY_PATH}:${PATH}


### Test

```shell
build_dir=$(mktemp -d /tmp/teradata-cpt-jni.XXXXXX)
cmake -S . -B "$build_dir"
cmake --build "$build_dir"
ctest --test-dir "$build_dir" --output-on-failure
```

测试覆盖完整字段、多个子区间以及包含 `0x00` 的二进制输入。多区间接口显式接收输入字节长度；`position` 和 `length` 均为从 0 开始的字节单位，最多支持 5 个子策略。

unit test the gbk
```shell
./cmake-build-debug/simple_single_test 1234 $(tail -n 1 test/gbk_input.txt)
```
