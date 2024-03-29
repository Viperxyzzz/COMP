# Compilers Project

For this project, you need to install [Java](https://jdk.java.net/), [Gradle](https://gradle.org/install/), and [Git](https://git-scm.com/downloads/) (and optionally, a [Git GUI client](https://git-scm.com/downloads/guis), such as TortoiseGit or GitHub Desktop). Please check the [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) for Java and Gradle versions.

## GROUP: <identifier of the group>

NAME1: Guilherme Garrido, NR1: 201905407, GRADE1: 18, CONTRIBUTION1: 25%

NAME2: Luís Lucas, NR2: 201904624, GRADE2: 17, CONTRIBUTION2: 25%

NAME3: Óscar Esteves, NR3: 201906834, GRADE3: 18, CONTRIBUTION3: 25%

NAME4: Pedro Nunes, NR4: 201905396, GRADE4: 18, CONTRIBUTION4: 25%

GLOBAL Grade of the project: 18


## SUMMARY:

This tool translates programs written in Java-- into java bytecodes, passing through different stages. First, it detectes syntactic errors using a well defined grammar. Then it generates an AST that is semantically analysed, producing a symbol table. From the AST, Ollir code is generated and, finally, from that, the jasmin code is produced.

This Java-- compiler can be used by running `gradle installDist`, which compiles and installs the program, and `comp2022-5a [-r=<num>] [-o] [-d] -i=<input_file.jmm>` to run it. The '-r' and '-o' options for register allocation and optimization, respectively, were not implemented. The '-d' option outputs the AST, Ollir and Jasmin codes generated by the tool.

## SEMANTIC ANALYSIS: 

Semantic rules implemented:

* Variables must be declared
* Operands must be compatible with the operation (int for arithmetic and comparisons, boolean for logical)
* Expressions in conditions must return a boolean
* Negation (!) can only be used for boolean expressions
* Length can only be used for arrays
* Arrays cannot be used in arithmetic operations
* Array access must be done over an array
* Array access index must be an expression of type int
* Non-static symbols can´t be accessed from static methods
* Assignments can only be done to variables (left-hand side must a variable)
* The type of the assignee must be compatible with the assigned
* Returned expression type of a method must be compatible with the return type defined in the method declaration
* When calling methods of the class declared in the code, types and number of arguments in the call must compatible with the ones in the method declaration
* Calling undeclared methods of the class declared in the code can only be done if the class extends another class
* Calling undeclared methods from other class can only be done if the class is imported


## CODE GENERATION:

### Abstract Syntax Tree - AST

If the code respects the defined grammar, an AST is generated, structuring everything needed in the following stages, like types, declarations, expressions, operations and identifiers. This information is used for building the symbol table, semantic analysis and for generating the Ollir code.

### Ollir

Using both the symbol table and the AST we generate OLLIR code. The main OLLIR generator is OllirGenerator with help of an auxiliar class, OllirUtils. Our generator is able to:

* Class Declarations
* Method Declarations
* Parsing to OLLIR data types
* Arithmetics
* Logical Operations
* Method Calls
* While and If statements
* Arrays creation and indexing
* Three adresses parsing
* Creation of temporary variables

Regarding the creation of temporary variables, they are always created whereas they're needed or not.

### Jasmin

For jasmin we use OLLIR code. The main Jasmin generator is OllirToJasmin and with it we're able to parse to:

* Class Declarations
* Call Instructions
* Return Instructions
* Conditional Instructions
* Arithmentic Instructions
* Assignment Instructions
* Limit Locals

However we don't support:
* Arrays


## PROS: (Identify the most positive aspects of your tool)

We were able to write a grammar that, along with semantic analysis, detects any Java-- language error, whether syntactic or semantic. 

We built a functional and well-structured AST that contains everything needed in the next phases, respecting every rule, like operator precedence.

The ollir is well functional, three adresses parsing works well and every other feature was implemented successfully.

The jasmin code is for the most part functional and follows the specifications.

## CONS: (Identify the most negative aspects of your tool)

OLLIR Code could be more modular, it was scalable but we repeat code, specially in the types identification. Other negative aspect is the creation of a temporary variable without deciding if it's really needed or not.

Due to some constraints in time we were not able to sucesfully implement arrays on Jasmin.

The tool has no optimizations or register allocation.

## Project setup

There are three important subfolders inside the main folder. First, inside the subfolder named ``javacc`` you will find the initial grammar definition. Then, inside the subfolder named ``src`` you will find the entry point of the application. Finally, the subfolder named ``tutorial`` contains code solutions for each step of the tutorial. JavaCC21 will generate code inside the subfolder ``generated``.

## Compile and Running

To compile and install the program, run ``gradle installDist``. This will compile your classes and create a launcher script in the folder ``./build/install/comp2022-00/bin``. For convenience, there are two script files, one for Windows (``comp2022-00.bat``) and another for Linux (``comp2022-00``), in the root folder, that call tihs launcher script.

After compilation, a series of tests will be automatically executed. The build will stop if any test fails. Whenever you want to ignore the tests and build the program anyway, you can call Gradle with the flag ``-x test``.

## Test

To test the program, run ``gradle test``. This will execute the build, and run the JUnit tests in the ``test`` folder. If you want to see output printed during the tests, use the flag ``-i`` (i.e., ``gradle test -i``).
You can also see a test report by opening ``./build/reports/tests/test/index.html``.

## Checkpoint 1
For the first checkpoint the following is required:

1. Convert the provided e-BNF grammar into JavaCC grammar format in a .jj file
2. Resolve grammar conflicts, preferably with lookaheads no greater than 2
3. Include missing information in nodes (i.e. tree annotation). E.g. include the operation type in the operation node.
4. Generate a JSON from the AST

### JavaCC to JSON
To help converting the JavaCC nodes into a JSON format, we included in this project the JmmNode interface, which can be seen in ``src-lib/pt/up/fe/comp/jmm/ast/JmmNode.java``. The idea is for you to use this interface along with the Node class that is automatically generated by JavaCC (which can be seen in ``generated``). Then, one can easily convert the JmmNode into a JSON string by invoking the method JmmNode.toJson().

Please check the JavaCC tutorial to see an example of how the interface can be implemented.

### Reports
We also included in this project the class ``src-lib/pt/up/fe/comp/jmm/report/Report.java``. This class is used to generate important reports, including error and warning messages, but also can be used to include debugging and logging information. E.g. When you want to generate an error, create a new Report with the ``Error`` type and provide the stage in which the error occurred.


### Parser Interface

We have included the interface ``src-lib/pt/up/fe/comp/jmm/parser/JmmParser.java``, which you should implement in a class that has a constructor with no parameters (please check ``src/pt/up/fe/comp/CalculatorParser.java`` for an example). This class will be used to test your parser. The interface has a single method, ``parse``, which receives a String with the code to parse, and returns a JmmParserResult instance. This instance contains the root node of your AST, as well as a List of Report instances that you collected during parsing.

To configure the name of the class that implements the JmmParser interface, use the file ``config.properties``.

### Compilation Stages 

The project is divided in four compilation stages, that you will be developing during the semester. The stages are Parser, Analysis, Optimization and Backend, and for each of these stages there is a corresponding Java interface that you will have to implement (e.g. for the Parser stage, you have to implement the interface JmmParser).


### config.properties

The testing framework, which uses the class TestUtils located in ``src-lib/pt/up/fe/comp``, has methods to test each of the four compilation stages (e.g., ``TestUtils.parse()`` for testing the Parser stage). 

In order for the test class to find your implementations for the stages, it uses the file ``config.properties`` that is in root of your repository. It has four fields, one for each stage (i.e. ``ParserClass``, ``AnalysisClass``, ``OptimizationClass``, ``BackendClass``), and initially it only has one value, ``pt.up.fe.comp.SimpleParser``, associated with the first stage.

During the development of your compiler you will update this file in order to setup the classes that implement each of the compilation stages.
