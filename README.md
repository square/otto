Otto - An event bus by Square
=============================

An enhanced Guava-based event bus with emphasis on Android support.

Otto is an event bus designed to decouple different parts of your application
while still allowing them to communicate efficiently.

Forked from Guava, Otto adds unique functionality to an already refined event
bus as well as specializing it to the Android platform.

*For usage instructions please see [the website][1].*



Download
--------

Downloadable .jars can be found on the [GitHub download page][2].

You can also depend on the .jar through Maven:
```xml
<dependency>
  <groupId>com.squareup</groupId>
  <artifactId>otto</artifactId>
  <version>1.3.6</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.squareup:otto:1.3.6'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].



License
-------

    Copyright 2012 Square, Inc.
    Copyright 2010 Google, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.



 [1]: http://square.github.com/otto/
 [2]: http://github.com/square/otto/downloads
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/
