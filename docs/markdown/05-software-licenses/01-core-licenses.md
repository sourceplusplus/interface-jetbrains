# Overview

The Source++ Core is the central component used to coordinate the Source++ Agent as well as keep the Source++ Plugin and Source++ Portal in sync with desired application data. The core also relies heavily on Apache SkyWalking and requires an Apache SkyWalking OAP server to function. The core is the only component in Source++ which must remain consistently online and available.

# Dependencies

| Library                                                     | License                                                             |
| ----------------------------------------------------------  | ------------------------------------------------------------------- |
| [Apache Commons](http://commons.apache.org/)                | [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)    |
| [Apache SkyWalking](http://skywalking.io/)                  | [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)    |
| [Dropwizard Metrics](https://github.com/dropwizard/metrics) | [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)    |
| [Eclipse Vert.x](http://vertx.io/)                          | [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)    |
| [Groovy](https://github.com/apache/groovy)                  | [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)    |
| [Gson](https://github.com/google/gson/)                     | [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)    |
| [Guava](https://github.com/google/guava)                    | [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)    |
| [Immutables](https://immutables.github.io/)                 | [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)    |
| [Jackson](https://github.com/codehaus/jackson)              | [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)    |
| [OpenTracing API](http://opentracing.io/)                   | [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)    |
| [SLF4J](http://www.slf4j.org/)                              | [The MIT License (MIT)](https://opensource.org/licenses/MIT)        |
