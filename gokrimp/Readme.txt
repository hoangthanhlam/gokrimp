Compile and run the command with one parameter: the database name.

Database contains two files: dataname.dat and dataname.lab

dataname.dat: multiple lines each corresponds to a sequence
dataname.lab: each line contains a label for the  event with id corresponds to the line number.

dataname.lab is optional, the program can run without this file.

An example of input data is included : jmlr.dat and jmlr.lab.

java -jar GoKrimp.jar jmlr


* NOTICE that the GoKrimp algorithm uses the SignTest to test for dependency, it requires that an event occurs at least in N=25 sequence to perform the test. If you have a very long sequence instead of a database of many sequences, you should split the long sequences into a set of short sequences.

* NOTICE that the source code contains SeqKrimp algorithm implementation which gets the candidate set and returns a good  subset of compressing patterns. You can feed SeqKrimp with any frequent pattern mining algorithm. In our implementation we use BIDE from Siemens but we are not allowed to redistribute BIDE so you must find another replacement, e.g. PrefixSpan.  
