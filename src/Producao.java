import java.util.*;
// Classe para representar uma Produção
class Producao {
    String esquerda;
    List<String> direita;

    // Construtor
    public Producao(String esquerda, List<String> direita) {
        this.esquerda = esquerda;
        this.direita = direita;
    }
    
    // Método para converter a produção para formato de string
    @Override
    public String toString() {
        return esquerda + " -> " + String.join(" | ", direita);
    }
}