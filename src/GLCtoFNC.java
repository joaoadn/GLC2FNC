import java.io.*;
import java.util.*;

// Classe principal para conversão de GLC para FNC
public class GLCtoFNC {

    // Conjunto para armazenar variáveis
    private Set<String> variaveis = new HashSet<>();
    // Conjunto para armazenar terminais
    private Set<String> terminais = new HashSet<>();
    // Lista de produções
    private List<Producao> producoes = new ArrayList<>();
    // Mapa para armazenar variáveis temporárias
    private Map<String, String> variaveisTemporarias = new HashMap<>();

    // Variável para armazenar o nome da variável inicial
    private String variavelInicial;

    // Método principal para iniciar a conversão
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Uso: java GLCtoFNC <entrada> <saida>");
            System.exit(1);
        }

        String arquivoEntrada = args[0];
        String arquivoSaida = args[1];

        try {
            GLCtoFNC conversor = new GLCtoFNC();
            conversor.lerArquivo(arquivoEntrada);
            conversor.adicionarVariavelInicial();
            conversor.eliminarProducoesNaoNormais();
            conversor.converterParaFNC(); // Certifique-se de que este método está chamado corretamente
            conversor.salvarArquivo(arquivoSaida);
        } catch (IOException e) {
            System.err.println("Erro ao processar arquivos: " + e.getMessage());
        }
    }

    // Método para ler o arquivo de entrada e armazenar as produções
    private void lerArquivo(String arquivo) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(arquivo));
        String linha;

        while ((linha = br.readLine()) != null) {
            linha = linha.trim();
            if (linha.isEmpty() || linha.startsWith("#")) continue;

            String[] partes = linha.split("->");
            if (partes.length != 2) continue;

            String esquerda = partes[0].trim();
            String[] direitos = partes[1].split("\\|");
            List<String> listaDireita = new ArrayList<>();
            for (String direito : direitos) {
                listaDireita.add(direito.trim());
                for (char c : direito.trim().toCharArray()) {
                    if (Character.isLowerCase(c)) {
                        terminais.add(String.valueOf(c));
                    } else if (Character.isUpperCase(c)) {
                        variaveis.add(String.valueOf(c));
                    }
                }
            }
            producoes.add(new Producao(esquerda, listaDireita));
        }
        br.close();
    }

    // Método para adicionar uma variável inicial, se necessário
    private void adicionarVariavelInicial() {
        // Se a variável inicial já está definida, não faz nada
        if (variaveis.contains("S")) return;

        // Cria uma nova variável inicial e adiciona produções
        String novaVariavelInicial = "S";
        variaveis.add(novaVariavelInicial);

        List<String> novasProducoes = new ArrayList<>();
        novasProducoes.add(novaVariavelInicial);
        novasProducoes.add(producoes.get(0).esquerda);

        producoes.add(0, new Producao(novaVariavelInicial, Collections.singletonList(producoes.get(0).esquerda)));
    }

    // Método para eliminar produções não normais
    private void eliminarProducoesNaoNormais() {
        // Implementar a eliminação de produções de comprimento maior que 2
        // e substituí-las por variáveis intermediárias
        eliminarProducoesUnitarias();
        eliminarProducoesComprimentoMaiorQue2();
    }

    // Método para eliminar produções unitárias
    private void eliminarProducoesUnitarias() {
        boolean alterado;
        do {
            alterado = false;
            Map<String, List<String>> novasProducoes = new HashMap<>();
            
            for (Producao p : producoes) {
                List<String> novasDireitas = new ArrayList<>();
                for (String direita : p.direita) {
                    if (direita.length() == 1 && variaveis.contains(direita)) {
                        // Substituir produções unitárias
                        for (Producao p2 : producoes) {
                            if (p2.esquerda.equals(direita)) {
                                novasDireitas.addAll(p2.direita);
                                alterado = true;
                            }
                        }
                    } else {
                        novasDireitas.add(direita);
                    }
                }
                if (novasDireitas.size() > 0) {
                    novasProducoes.put(p.esquerda, novasDireitas);
                }
            }

            producoes.clear();
            for (Map.Entry<String, List<String>> entry : novasProducoes.entrySet()) {
                producoes.add(new Producao(entry.getKey(), entry.getValue()));
            }
        } while (alterado);
    }

    // Método para eliminar produções de comprimento maior que 2
    private void eliminarProducoesComprimentoMaiorQue2() {
        List<Producao> novasProducoes = new ArrayList<>();
        int contador = 1;

        for (Producao p : producoes) {
            for (String direita : p.direita) {
                if (direita.length() > 2) {
                    String restante = direita;
                    String anterior = restante.substring(0, 2);

                    while (restante.length() > 2) {
                        String novaVariavel = "T" + (contador++);
                        variaveis.add(novaVariavel);

                        novasProducoes.add(new Producao(novaVariavel, Collections.singletonList(restante.substring(1))));
                        restante = restante.substring(1);
                        anterior = restante.substring(0, 2);
                    }
                    novasProducoes.add(new Producao(p.esquerda, Collections.singletonList(anterior)));
                } else {
                    novasProducoes.add(new Producao(p.esquerda, Collections.singletonList(direita)));
                }
            }
        }

        producoes = novasProducoes;
    }

    // Método para garantir que todas as produções estejam na forma A -> BC ou A -> a
    private void garantirFormaFNC() {
        List<Producao> novasProducoes = new ArrayList<>();
        int contador = 1;

        for (Producao p : producoes) {
            for (String direita : p.direita) {
                if (direita.length() > 2) {
                    String novaVariavel = "T" + (contador++);
                    variaveis.add(novaVariavel);

                    List<String> novaDireita = new ArrayList<>();
                    novaDireita.add(direita.substring(0, 2));
                    novasProducoes.add(new Producao(p.esquerda, novaDireita));

                    producoes.add(new Producao(novaVariavel, Collections.singletonList(direita.substring(2))));
                } else if (direita.length() == 2) {
                    novasProducoes.add(new Producao(p.esquerda, Collections.singletonList(direita)));
                } else {
                    novasProducoes.add(new Producao(p.esquerda, Collections.singletonList(direita)));
                }
            }
        }

        producoes = novasProducoes;
    }

    // Método para converter a gramática para FNC
    private void converterParaFNC() {
        garantirFormaFNC();
        // Outros métodos de conversão podem ser chamados aqui, se necessário
    }

    // Método para salvar a gramática em FNC em um arquivo de saída
    private void salvarArquivo(String arquivo) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(arquivo));
        for (Producao producao : producoes) {
            bw.write(producao.toString());
            bw.newLine();
        }
        bw.close();
    }
}






