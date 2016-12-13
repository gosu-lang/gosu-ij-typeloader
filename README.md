# gosu-ij-typeloader
A plugin for JetBrains IntelliJ which enables editor support for Gosu's pluggable Type System.
 
## Instructions
There are two ways to launch an IJ sandbox environment: 
 * From the command line, execute `$ ./gradlew runIdea`
 * From IJ, run the `runIdea` Gradle task

This will launch a sandbox with this plugin and the [OS Gosu plugin](https://plugins.jetbrains.com/plugin/7140).

From this point, open a new project or point the sandbox to a project which contains an implementation of `gw.lang.reflect.TypeLoaderBase`.  The [gosu-xml](https://github.com/gosu-lang/gosu-pl/tree/master/gosu-xml) project is an example of such a typeloader implementation.