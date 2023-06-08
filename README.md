# ProcessBuilderCppCompile
The CppComp file creates an abstraction of processes for dealing wiht C++ code inside Java. It uses ProcessBuilder class to do the necessary operations.

It runs a C++ code given in a string or a file and automatically directs the output of the running process to std.out and is able to write to the process using the sendInput method.

The main file shows an example usage of the class. It currently runs the aa.cpp program and sends three inputs to it. Running bb.cpp is more straight forward and can be done by changing the file name and removing the scanner lines.
