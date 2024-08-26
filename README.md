# GLC2FNC

Programa em Java capaz de converter uma Gramática Livre de Contexto, em um arquivo texto de entrada, para a Forma Normal de Chomsky, em um arquivo texto de saída. 

## Convert a Context Free Grammar to Chomsky Normal Form

Java program to convert a Context Free Grammar to Chomsky Normal Form, manipulating archives to read the input and generate an output archive with the Chomsky Normal Form.


### Using

```
git clone https://github.com/joaoadn/GLC2FNC
javac GLCtoFNC.java
java GLCtoFNC glc1.txt fnc.txt
```

### Input

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


