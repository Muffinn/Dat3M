# Dat3M: Memory Model Aware Verification

<p align="center"> 
<img src="ui/src/main/resources/dat3m.png">
</p>

This tool suite is currently composed of two tools.

* **Dartagnan:** a tool to check state reachability under weak memory models.
* **Porthos:** a tool to check state inclusion under weak memory models.

Requirements
======
* [Maven](https://maven.apache.org/)
* [SMACK](https://github.com/smackers/smack)

Installation
======
Set the path and shared libraries variables (replace the latter by DYLD_LIBRARY_PATH in **MacOS**):
```
export PATH=<Dat3M's root>/:$PATH
export LD_LIBRARY_PATH=<Dat3M's root>/lib/:$LD_LIBRARY_PATH
```

To build the tools, from the Dat3M's root directory run
* **Linux:**
```
make
```
* **MacOS:**
```
mvn install:install-file -Dfile=lib/z3-4.3.2.jar -DgroupId=com.microsoft -DartifactId="z3" -Dversion=4.3.2 -Dpackaging=jar
mvn clean install -DskipTests
```

Unit Tests
======
We provide a set of unit tests that can be run by
```
mvn test
```

Binaries
======
The precompiled jars can be found in the [release](https://github.com/hernanponcedeleon/Dat3M/releases) section.

Usage
======
Dat3M comes with a user interface (UI) where it is easy the select the tool to use (Dartagnan or Porthos), import, export and modify both the program and the memory model and select the options for the verification engine (see below).
You can start the UI by:

* **Linux:** double-clicking the <img src="ui/src/main/resources/dat3m.png" width="30" height="30"> launcher
* **MacOS:** running
```
java -jar ui/target/ui-2.0.6-jar-with-dependencies.jar
```
<p align="center"> 
<img src="ui/src/main/resources/ui.jpg">
</p>

DARTAGNAN supports programs written in the .litmus or .bpl (Boogie) formats. For PORTHOS, programs shall be written in the .pts format which is explained [here](porthos/pts.md).

If SMACK was correctly installed, C programs can be converted to Boogie using the following script:
```
c2bpl.sh <C file> <new Boogie file>
```

Additionally, you can run DARTAGNAN and PORTHOS from the console.

For checking reachability:
```
java -jar dartagnan/target/dartagnan-2.0.6-jar-with-dependencies.jar -cat <CAT file> -i <program file> [options]
```
For checking state inclusion:
```
java -jar porthos/target/porthos-2.0.6-jar-with-dependencies.jar -s <source> -scat <CAT file> -t <target> -tcat <CAT file> -i <program file> [options]
```
The -cat,-scat,-tcat options specify the paths to the CAT files.

For programs written in the .pts format, \<source> and \<target> specify the architectures to which the program will be compiled. 
They must be one of the following: 
- none
- tso
- power
- arm
- arm8

Other optional arguments include:
- -m, --mode {knastertarski, idl, kleene}: specifies the encoding for fixed points. Knaster-tarski (default mode) uses the encoding introduced in [2]. Mode idl uses the Integer Difference Logic iteration encoding introduced in [1]. Kleene mode uses the Kleene iteration encoding using one Boolean variable for each iteration step.
- -a, --alias {none, andersen, cfs}: specifies the alias-analysis used. Option andersen (the default one) uses a control-flow-insensitive method. Option cfs uses a control-flow-sensitive method. Option none performs no alias analysis.
- -unroll: unrolling bound for the BMC.

Dartagnan supports input non-determinism, assumptions and assertions using the [SVCOMP](https://sv-comp.sosy-lab.org/2020/index.php) commands "__VERIFIER_nondet_X", "__VERIFIER_assume" and "__VERIFIER_assert".

Authors and Contact
======
**Maintainer:**

* [Hernán Ponce de León](mailto:ponce@fortiss.org)

**Former Developers:**

* [Florian Furbach](mailto:f.furbach@tu-braunschweig.de)
* [Natalia Gavrilenko](mailto:natalia.gavrilenko@aalto.fi)

Please feel free to contact us in case of questions or to send feedback.

References
======
[1] Hernán Ponce de León, Florian Furbach, Keijo Heljanko, Roland Meyer: **Portability Analysis for Weak Memory Models. PORTHOS: One Tool for all Models**. SAS 2017.

[2] Hernán Ponce de León, Florian Furbach, Keijo Heljanko, Roland Meyer: **BMC with Memory Models as Modules**. FMCAD 2018.

[3] Natalia Gavrilenko, Hernán Ponce de León, Florian Furbach, Keijo Heljanko, Roland Meyer: **BMC for Weak Memory Models: Relation Analysis for Compact SMT Encodings**. CAV 2019.

[4] Hernán Ponce de León, Florian Furbach, Keijo Heljanko, Roland Meyer: **Dartagnan: Bounded Model Checking for Weak Memory Models (Competition Contribution)**. TACAS 2020.
