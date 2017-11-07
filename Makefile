default: all

run_group_server:
	(cd build && java RunGroupServer)

run_file_server:
	(cd build && java RunFileServer)

test_client:
	(cd build && java RunClient localhost 8765 localhost 4321 test)

run_client:
	(cd build && java RunClient localhost 8765 localhost 4321)

all:
	mkdir -p build
	javac -d build src/*.java

clean:
	rm -f build/*.class
	rm -f build/*.bin
	rm -rf build/shared_files