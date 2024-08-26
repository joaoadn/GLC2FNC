# GLC2FNC

Programa em Java capaz de converter uma Gramática Livre de Contexto para a Forma Normal de Chomsky.

## Convert a Context Free Grammar to Chomsky Normal Form

Java program to convert a Context Free Grammar to Chomsky Normal Form, manipulating archives to read the input and create an output archive with the Chomsky Normal Form.


### Using

```
Create a file with the Input.
git clone https://github.com/joaoadn/GLC2FNC
javac GLCtoFNC.java
java GLCtoFNC [inputFile] [outputFile] 
```

### Input [glc1.txt]

```
S -> aS | bS | C | D
C -> c | .
D -> abc
D -> .

```

FNC (CNF):

```
S' -> BS | a | AS | b | AT1 | c | .
S -> BS | a | AS | b | AT1 | c
A -> a
B -> b
C -> c
T1 -> BC

```


