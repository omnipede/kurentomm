# kurentomm

Kurento Magic Mirror demo

# Requirements

* Docker
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

* Run application server
```shell
$ ./gradlew bootRun -x test
```
