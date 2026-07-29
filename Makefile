SHELL := /bin/bash
.DEFAULT_GOAL := help

MVN ?= mvn
CMAKE ?= cmake
CTEST ?= ctest
BUILD_TYPE ?= Release
NATIVE_BUILD_DIR ?= target/native-make
ASAN_BUILD_DIR ?= target/native-asan

.PHONY: help compile build test package clean \
        native-configure native-build native-test native-asan \
        java-compile java-test java-package

help: ## 显示可用命令
	@printf '%s\n' \
	  'Teradata CPT 构建命令' \
	  '' \
	  '  make compile          编译 C/C++ 原生库和 Java 代码' \
	  '  make build            编译并执行全部 C/Java 测试' \
	  '  make test             执行原生 CTest 和 Java/JNI 测试' \
	  '  make package          测试并生成完整 JAR' \
	  '  make clean            清理 Maven/CMake 构建产物' \
	  '' \
	  '  make native-build     仅构建原生库和 C 测试程序' \
	  '  make native-test      仅执行原生 CTest' \
	  '  make native-asan      使用 ASan/UBSan 构建并测试原生代码' \
	  '  make java-compile     仅编译 Java（跳过自动原生构建）' \
	  '  make java-test        使用已有动态库执行 Java 测试' \
	  '  make java-package     使用已有动态库生成 JAR' \
	  '' \
	  '可覆盖变量：' \
	  '  BUILD_TYPE=Debug      原生构建类型，默认 Release' \
	  '  NATIVE_BUILD_DIR=...  原生构建目录，默认 target/native-make'

native-configure: ## 配置 CMake 原生工程
	$(CMAKE) -S native -B "$(NATIVE_BUILD_DIR)" -DCMAKE_BUILD_TYPE="$(BUILD_TYPE)"

native-build: native-configure ## 构建 C/C++ 原生库和测试程序
	$(CMAKE) --build "$(NATIVE_BUILD_DIR)" --config "$(BUILD_TYPE)"

native-test: native-build ## 执行所有 CTest 原生测试
	$(CTEST) --test-dir "$(NATIVE_BUILD_DIR)" --output-on-failure

native-asan: ## 使用 AddressSanitizer/UBSan 构建并测试
	$(CMAKE) -S native -B "$(ASAN_BUILD_DIR)" \
	  -DCMAKE_BUILD_TYPE=Debug \
	  -DCMAKE_C_FLAGS='-fsanitize=address,undefined -fno-omit-frame-pointer' \
	  -DCMAKE_CXX_FLAGS='-fsanitize=address,undefined -fno-omit-frame-pointer' \
	  -DCMAKE_EXE_LINKER_FLAGS='-fsanitize=address,undefined' \
	  -DCMAKE_SHARED_LINKER_FLAGS='-fsanitize=address,undefined'
	$(CMAKE) --build "$(ASAN_BUILD_DIR)"
	ASAN_OPTIONS=detect_leaks=1 $(CTEST) --test-dir "$(ASAN_BUILD_DIR)" --output-on-failure

java-compile: ## 仅编译 Java，使用 resources 中已有动态库
	$(MVN) -P\!native -DskipTests compile

java-test: ## 仅执行 Java/JNI 测试，使用已有动态库
	$(MVN) -P\!native test

java-package: ## 仅执行 Java 打包，使用已有动态库
	$(MVN) -P\!native -DskipTests package

compile: ## 通过 Maven 自动构建原生库并编译 Java
	$(MVN) -DskipTests compile

build: ## 编译并执行全部 C/Java 测试
	$(MVN) clean test

test: build ## build 的便捷别名

package: ## 执行全部测试并生成完整 JAR
	$(MVN) clean package

clean: ## 清理所有 Maven 和独立 CMake 构建产物
	$(MVN) clean
