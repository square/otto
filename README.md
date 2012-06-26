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

    <dependency>
        <groupId>com.squareup</groupId>
        <artifactId>otto<artifactId>
        <version>(insert latest version)</version>
    </dependency>



Contributing
------------

If you would like to contribute code to Otto you can do so through GitHub by
forking the repository and sending a pull request.

When submitting code, please make every effort to follow existing conventions
and style in order to keep the code as readable as possible. Please also make
sure your code compiles by running `mvn clean verify`. Checkstyle failures
during compilation indicate errors in your style and can be viewed in the
`checkstyle-result.xml` file.

Before your code can be accepted into the project you must also sign the
[Individual Contributor License Agreement (CLA)][3].



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
 [3]: https://spreadsheets.google.com/spreadsheet/viewform?formkey=dDViT2xzUHAwRkI3X3k5Z0lQM091OGc6MQ&ndplr=1