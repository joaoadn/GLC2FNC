import java.io.*;
import java.util.*;
public class GLCtoFNC {

    private Set<String> variaveis = new HashSet<>(); // Conjunto de variáveis (não terminais)
    private Set<String> terminais = new HashSet<>(); // Conjunto de terminais
    private List<Producao> producoes = new ArrayList<>(); // Lista de produções
    private String variavelInicial = "S"; // Variável inicial da gramática
    private int contadorTemporario = 1; // Contador para gerar variáveis temporárias

    public static void main(String[] args) {
        // Verifica se o número correto de argumentos foi fornecido
        if (args.length != 2) {
            System.out.println("Uso: java GLCtoFNC <entrada> <saida>");
            System.exit(1);
        }

        String arquivoEntrada = args[0];
        String arquivoSaida = args[1];

        try {
            GLCtoFNC conversor = new GLCtoFNC();
            conversor.lerArquivo(arquivoEntrada); // Lê a gramática do arquivo de entrada
            conversor.adicionarVariavelInicial(); // Adiciona uma variável inicial, se necessário
            conversor.eliminarProducoesNaoNormais(); // Elimina produções não normais
            conversor.converterParaFNC(); // Converte a gramática para FNC
            conversor.salvarArquivo(arquivoSaida); // Salva a gramática na forma normal de Chomsky no arquivo de saída
        } catch (IOException e) {
            System.err.println("Erro ao processar arquivos: " + e.getMessage());
        }
    }

    // Lê o arquivo de entrada e armazena as produções
    private void lerArquivo(String arquivo) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(arquivo));
        String linha;

        while ((linha = br.readLine()) != null) {
            linha = linha.trim();
            if (linha.isEmpty() || linha.startsWith("#")) continue; // Ignora linhas vazias e comentários

            String[] partes = linha.split("->");
            if (partes.length != 2) continue; // Ignora linhas mal formatadas

            String esquerda = partes[0].trim();
            String[] direitos = partes[1].split("\\|");
            List<String> listaDireita = new ArrayList<>();
            for (String direito : direitos) {
                listaDireita.add(direito.trim());
                // Identifica terminais e variáveis
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

    // Adiciona uma nova variável inicial, se necessário
    private void adicionarVariavelInicial() {
        if (variaveis.contains("S")) return; // Se já existe uma variável inicial, nada a fazer

        String novaVariavelInicial = "S'";
        variaveis.add(novaVariavelInicial);

        // Adiciona uma produção inicial que gera a variável inicial original
        List<String> novasProducoes = new ArrayList<>();
        novasProducoes.add(novaVariavelInicial);
        novasProducoes.add(variavelInicial);

        producoes.add(0, new Producao(novaVariavelInicial, Collections.singletonList(variavelInicial)));
    }

    // Elimina produções não normais da gramática
    private void eliminarProducoesNaoNormais() {
        eliminarProducoesUnitarias(); // Remove produções unitárias
        eliminarProducoesComComprimentoMaiorQue2(); // Remove produções com comprimento maior que 2
    }

    // Elimina produções unitárias (A -> B)
    private void eliminarProducoesUnitarias() {
        boolean alterado;
        do {
            alterado = false;
            Map<String, List<String>> novasProducoes = new HashMap<>();

            for (Producao p : producoes) {
                List<String> novasDireitas = new ArrayList<>();
                for (String direita : p.direita) {
                    if (direita.length() == 1 && variaveis.contains(direita)) {
                        // Substitui produções unitárias pelas produções da variável referenciada
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

    // Elimina produções com comprimento maior que 2
    private void eliminarProducoesComComprimentoMaiorQue2() {
        List<Producao> novasProducoes = new ArrayList<>();

        for (Producao p : producoes) {
            for (String direita : p.direita) {
                if (direita.length() > 2) {
                    // Quebra produções de comprimento maior que 2 usando variáveis temporárias
                    String restante = direita;
                    String novaVariavel = "T" + (contadorTemporario++);
                    variaveis.add(novaVariavel);

                    while (restante.length() > 2) {
                        novasProducoes.add(new Producao(novaVariavel, Collections.singletonList(restante.substring(0, 2))));
                        restante = restante.substring(1);
                        novaVariavel = "T" + (contadorTemporario++);
                        variaveis.add(novaVariavel);
                    }
                    novasProducoes.add(new Producao(novaVariavel, Collections.singletonList(restante)));
                } else {
                    novasProducoes.add(new Producao(p.esquerda, Collections.singletonList(direita)));
                }
            }
        }

        producoes = novasProducoes;
    }

    // Converte a gramática para a forma normal de Chomsky (FNC)
    private void converterParaFNC() {
        garantirFormaFNC();
    }

    // Garante que todas as produções estejam na forma A -> BC ou A -> a
    private void garantirFormaFNC() {
        List<Producao> novasProducoes = new ArrayList<>();
        Set<String> variaveisAdicionais = new HashSet<>();
        
        for (Producao p : producoes) {
            for (String direita : p.direita) {
                if (direita.length() == 1 && terminais.contains(direita)) {
                    novasProducoes.add(new Producao(p.esquerda, Collections.singletonList(direita)));
                } else if (direita.length() == 2 && !direita.matches("[A-Z]{2}")) {
                    // Substitui produções de comprimento maior que 2 por variáveis temporárias
                    String restante = direita;
                    String novaVariavel = "T" + (contadorTemporario++);
                    variaveis.add(novaVariavel);
                    variaveisAdicionais.add(novaVariavel);
        
                    // Adiciona a primeira produção com a nova variável
                    List<String> novaDireita = new ArrayList<>();
                    novaDireita.add(restante.substring(0, 2));
                    novasProducoes.add(new Producao(p.esquerda, novaDireita));
        
                    while (restante.length() > 2) {
                        novaVariavel = "T" + (contadorTemporario++);
                        variaveis.add(novaVariavel);
                        variaveisAdicionais.add(novaVariavel);
                        novasProducoes.add(new Producao(novaVariavel, Collections.singletonList(restante.substring(0, 2))));
                        restante = restante.substring(1);
                    }
                    novasProducoes.add(new Producao(novaVariavel, Collections.singletonList(restante)));
                } else {
                    novasProducoes.add(new Producao(p.esquerda, Collections.singletonList(direita)));
                }
            }
        }
        
        // Substitui produções originais pelas novas
        producoes.clear();
        producoes.addAll(novasProducoes);
    
        // Atualiza o conjunto de variáveis
        variaveis.addAll(variaveisAdicionais);
    }
    
    
    // Salva a gramática convertida em FNC no arquivo de saída
    private void salvarArquivo(String arquivo) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(arquivo));
        for (Producao producao : producoes) {
            bw.write(producao.toString());
            bw.newLine();
        }
        bw.close();
    }
}







