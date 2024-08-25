import java.io.*;
import java.util.*;

public class GLCtoFNC {
    private Set<String> variaveis = new LinkedHashSet<>();
    private Set<String> terminais = new LinkedHashSet<>();
    private Map<String, List<String>> producoes = new LinkedHashMap<>();
    private String variavelInicial = "S";
    private int contadorTemporario = 1;

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
            conversor.adicionarTerminaisVariaveis();
            conversor.eliminarProducoesEpsilon();
            conversor.eliminarProducoesUnitarias();
            conversor.converterParaFNC();
            conversor.salvarArquivo(arquivoSaida);
        } catch (IOException e) {
            System.err.println("Erro ao processar arquivos: " + e.getMessage());
        }
    }

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
                String regra = direito.trim();
                if (regra.equals(".")) {
                    listaDireita.add(".");
                } else {
                    listaDireita.add(regra);
                    for (char c : regra.toCharArray()) {
                        if (Character.isLowerCase(c)) {
                            terminais.add(String.valueOf(c));
                        } else if (Character.isUpperCase(c)) {
                            variaveis.add(String.valueOf(c));
                        }
                    }
                }
            }
            producoes.put(esquerda, listaDireita);
        }
        br.close();
    }

    private void adicionarVariavelInicial() {
        if (variaveis.contains("S")) return;

        String novaVariavelInicial = "S'";
        variaveis.add(novaVariavelInicial);

        List<String> novasProducoes = new ArrayList<>();
        novasProducoes.add(variavelInicial);
        producoes.put(novaVariavelInicial, novasProducoes);
    }

    private void adicionarTerminaisVariaveis() {
        Map<String, String> substituicoesTemporarias = new LinkedHashMap<>();
        for (String terminal : terminais) {
            String variavelTemp = terminal.toUpperCase();
            if (!variaveis.contains(variavelTemp)) {
                variaveis.add(variavelTemp);
                substituicoesTemporarias.put(terminal, variavelTemp);
                producoes.put(variavelTemp, Collections.singletonList(terminal));
            }
        }
    }

    private void eliminarProducoesEpsilon() {
        Set<String> variaveisComEpsilon = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : producoes.entrySet()) {
            String esquerda = entry.getKey();
            for (String direita : entry.getValue()) {
                if (direita.equals(".")) {
                    variaveisComEpsilon.add(esquerda);
                }
            }
        }

        if (variaveisComEpsilon.isEmpty()) return;

        Map<String, List<String>> novasProducoes = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : producoes.entrySet()) {
            String esquerda = entry.getKey();
            List<String> novasDireitas = new ArrayList<>();
            for (String direita : entry.getValue()) {
                if (!direita.equals(".")) {
                    novasDireitas.add(direita);
                    for (int i = 0; i < direita.length(); i++) {
                        if (variaveisComEpsilon.contains(String.valueOf(direita.charAt(i)))) {
                            String novaDireita = direita.substring(0, i) + direita.substring(i + 1);
                            if (!novasDireitas.contains(novaDireita) && !novaDireita.equals("")) {
                                novasDireitas.add(novaDireita);
                            }
                        }
                    }
                }
            }
            if (!novasDireitas.isEmpty() || (esquerda.equals(variavelInicial) && variaveisComEpsilon.contains(esquerda))) {
                novasProducoes.put(esquerda, novasDireitas);
            }
        }

        producoes = novasProducoes;
    }

    private void eliminarProducoesUnitarias() {
        boolean alterado;
        do {
            alterado = false;
            Map<String, List<String>> novasProducoes = new LinkedHashMap<>();

            for (Map.Entry<String, List<String>> entry : producoes.entrySet()) {
                String esquerda = entry.getKey();
                List<String> novasDireitas = new ArrayList<>();
                for (String direita : entry.getValue()) {
                    if (direita.length() == 1 && variaveis.contains(direita)) {
                        List<String> substituicoes = producoes.get(direita);
                        if (substituicoes != null) {
                            for (String substituicao : substituicoes) {
                                if (!novasDireitas.contains(substituicao)) {
                                    novasDireitas.add(substituicao);
                                    alterado = true;
                                }
                            }
                        }
                    } else {
                        if (!novasDireitas.contains(direita)) {
                            novasDireitas.add(direita);
                        }
                    }
                }
                if (!novasDireitas.isEmpty()) {
                    novasProducoes.put(esquerda, novasDireitas);
                }
            }
            producoes.clear();
            producoes.putAll(novasProducoes);
        } while (alterado);
    }

    private void converterParaFNC() {
        Map<String, List<String>> novasProducoes = new LinkedHashMap<>();
        Map<String, String> substituicoesTemporarias = new LinkedHashMap<>();

        // Adiciona a produção para a nova variável inicial
        List<String> producoesSPrime = new ArrayList<>();
        producoesSPrime.add(variavelInicial);
        producoes.put("S'", producoesSPrime);

        for (Map.Entry<String, List<String>> entry : producoes.entrySet()) {
            String esquerda = entry.getKey();
            for (String direita : entry.getValue()) {
                if (direita.length() == 1 && terminais.contains(direita)) {
                    // Mapeia o terminal para uma variável correspondente
                    String variavelTemp = substituicoesTemporarias.get(direita);
                    if (variavelTemp != null) {
                        List<String> novaDireita = Collections.singletonList(direita);
                        if (!novasProducoes.containsKey(variavelTemp)) {
                            novasProducoes.put(variavelTemp, novaDireita);
                        }
                        List<String> producaoExistente = novasProducoes.get(esquerda);
                        if (producaoExistente == null) {
                            producaoExistente = new ArrayList<>();
                            novasProducoes.put(esquerda, producaoExistente);
                        }
                        if (!producaoExistente.contains(variavelTemp)) {
                            producaoExistente.add(variavelTemp);
                        }
                    }
                } else if (direita.length() > 2) {
                    // Divide a produção longa em produções binárias
                    String novaVariavel = "T" + contadorTemporario++;
                    List<String> novaDireita = new ArrayList<>();
                    novaDireita.add(direita.substring(0, 1));
                    novaDireita.add(direita.substring(1));
                    novasProducoes.put(novaVariavel, novaDireita);
                    List<String> producaoExistente = novasProducoes.get(esquerda);
                    if (producaoExistente == null) {
                        producaoExistente = new ArrayList<>();
                        novasProducoes.put(esquerda, producaoExistente);
                    }
                    if (!producaoExistente.contains(novaVariavel)) {
                        producaoExistente.add(novaVariavel);
                    }
                } else {
                    List<String> producaoExistente = novasProducoes.get(esquerda);
                    if (producaoExistente == null) {
                        producaoExistente = new ArrayList<>();
                        novasProducoes.put(esquerda, producaoExistente);
                    }
                    if (!producaoExistente.contains(direita)) {
                        producaoExistente.add(direita);
                    }
                }
            }
        }

        // Atualiza as produções com as variáveis temporárias
        producoes.putAll(novasProducoes);
    }

    private void salvarArquivo(String arquivo) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(arquivo));
        for (Map.Entry<String, List<String>> entry : producoes.entrySet()) {
            String esquerda = entry.getKey();
            for (String direita : entry.getValue()) {
                bw.write(esquerda + " -> " + direita);
                bw.newLine();
            }
        }
        bw.close();
    }
}


















































