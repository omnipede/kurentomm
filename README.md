# kurentomm

Kurento Magic Mirror demo

# Requirements

* jdk >= 19
* bower

# Run

* Start kurento server
```shell
$ sh ./start.sh
```

* Install bower js static dependency
```shell
$ cd src/main/resources/static
$ bower install
```

* Run server
```shell
$ ./gradlew bootRun -x test
```