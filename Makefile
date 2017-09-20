default: all

run:
	java -cp build RunGroupServer

all:
	mkdir -p build
	javac -d build src/*.java

clean:
	rm build/*.class
