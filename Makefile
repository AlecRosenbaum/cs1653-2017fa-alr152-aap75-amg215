default: all

run_group_server:
	(cd build && java RunGroupServer)

run_file_server:
	(cd build && java RunFileServer)

run_client:
	java -cp build RunClient localhost 8765 localhost 4321

all: clean
	mkdir -p build
	javac -d build src/*.java

clean:
	rm -f build/*.class
