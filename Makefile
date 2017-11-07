default: all

run_group_server:
	(cd build && java -cp .:../lib/bcprov-ext-jdk15on-158.jar RunGroupServer)

run_file_server:
	(cd build && java -cp .:../lib/bcprov-ext-jdk15on-158.jar RunFileServer)

test_client:
	(cd build && java -cp .:../lib/bcprov-ext-jdk15on-158.jar RunClient localhost 8765 localhost 4321 test)

run_client:
	(cd build && java -cp .:../lib/bcprov-ext-jdk15on-158.jar RunClient localhost 8765 localhost 4321)

all:
	mkdir -p build
	javac -cp lib/bcprov-ext-jdk15on-158.jar -d build src/*.java

clean:
	rm -f build/*.class
	rm -f build/*.bin
	rm -rf build/shared_files