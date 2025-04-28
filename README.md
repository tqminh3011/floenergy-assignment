# FloEnergy assignment
### Author: Minh Tran (Bryan)
### Tech stacks
- Language: Java17
- Build tool: Gradle
- Framework: SpringBoot 3
- Libs: OpenCSV, Logback
- Test: JUnit 5

### How to build and test?
#### 1. Build: 
```
./gradlew clean build
```

#### 2. Test:
2.1. Run built jar:
```
java -jar build/libs/floenergy-assignment-1.0-SNAPSHOT.jar
```
2.2. Enter path to input CSV file when asked `Please enter CSV file path:`</br>
(e.g: `src/test/resources/input/example.csv`)

### Assessment Write up 

#### Q1. What are the advantages of the technologies you used for the project?
**Java** is a mature OOP programming language with one of largest Dev communities. Some of its advantages:
- High-performant compare to other general purpose languages.
- Compile-time safety and platform independent.
- Strict syntax and robust test framework.


**Spring** framework helps us develop and manage code with production-grade quality via features like dependency injection, managed configuration properties (via YML file), DB support (Spring Data/JPA), AOP, integration tests (via SpringBootTest),...

#### Q2. How is the code designed and structured?
As this is a small half day assignment, not much of effort to redesign/restructure code with a fancy architecture. Basically this is simply what I applied:
* Split code into smaller classes like Factory, Processor.
* Within a class, separate code into multiple methods and expose some configurations to prop file (`application.yml`).

#### Q3. How does the design help to make the codebase readable and maintainable for other engineers?
* Splitting code into smaller classes, methods will help engineers separate code logic, therefore code is easier to maintain and test.
* It also helps reduce cognitive load for readers while going through the code, they can focus more on a specific part of code. 
* Exposing some configurable variables to properties file assists in better config management and seamless value updates.

#### Q4. Discuss any design patterns, coding conventions, or documentation practices you implemented to enhance readability and maintainability.
* **Factory design pattern** (in `MeterReadingFactory.java`), helps encapsulate object creation and improve testability.
* **DRY (Don't Repeat Yourself)** (splitting smaller classes methods), helps achieve cleaner code.
* **Centralized log pattern** (using Logback), helps bring more consistent and useful logs.
* **IoC (Inversion of Control)**: Framework (Spring) automatically manages and creates dependencies via Bean ( like `Nem12CsvProcessor`) so that our code is more testable and maintainable. 

#### Q5. What would you do better next time?
Improvements:
* Support concurrent processing of file (if order does not matter) to improve processing time
* Support processing multiple files (input can be folder)
* Add some NEM12 validations to make app more robust


#### Q6. Reflect on areas where you see room for improvement and describe how you would approach them differently in future projects.


#### Q7. What other ways could you have done this project?


#### Q8. Explore alternative approaches or technologies that you considered during the development of the project.
Can use Python to write a script as well. Python would help code less verbose and easier to read. However in the long run, Python is less performant and harder to maintain than Java.  
