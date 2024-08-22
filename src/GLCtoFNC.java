import java.io.*;
import java.util.ArrayList;

public class GLCtoFNC {

    // Método principal que faz a conversão de GLC para FNC
    public static void convertGLCtoFNC(String inputFilePath, String outputFilePath) {
        ArrayList<Producao> producoes = new ArrayList<>();
        ArrayList<Variavel> variaveis = new ArrayList<>();
        ArrayList<Terminal> terminais = new ArrayList<>();

        // Carrega a GLC do arquivo de entrada
        loadGLCFromFile(inputFilePath, producoes, variaveis, terminais);
        // Remove produções vazias
        removeEmptyProductions(producoes);
        // Substitui terminais por variáveis
        replaceTerminalsWithVariables(producoes, variaveis);
        // Garante que cada produção tenha apenas duas variáveis
        ensureTwoVariablesPerProduction(producoes, variaveis);
        // Salva a FNC no arquivo de saída
        saveFNCToFile(outputFilePath, producoes);
    }

    // Remove produções vazias
    private static void removeEmptyProductions(ArrayList<Producao> producoes) {
        producoes.removeIf(producao -> producao.getLadoDir().equals(".") || producao.getLadoDir().isEmpty());
    }    

    // Substitui terminais por variáveis
    private static void replaceTerminalsWithVariables(ArrayList<Producao> producoes, ArrayList<Variavel> variaveis) {
        for (Producao producao : producoes) {
            StringBuilder novoLadoDir = new StringBuilder();
            for (char simbolo : producao.getLadoDir().toCharArray()) {
                if (Character.isLowerCase(simbolo)) {
                    novoLadoDir.append(getOrCreateVariable(variaveis, simbolo));
                } else {
                    novoLadoDir.append(simbolo);
                }
            }
            producao.setLadoDir(novoLadoDir.toString());
        }
    }    

    // Garante que cada produção tenha no máximo duas variáveis no lado direito
    private static void ensureTwoVariablesPerProduction(ArrayList<Producao> producoes, ArrayList<Variavel> variaveis) {
        for (int i = 0; i < producoes.size(); i++) {
            Producao producao = producoes.get(i);
            while (producao.getLadoDir().length() > 2) {
                String novaVar = generateNewVariable(variaveis);
                producoes.add(new Producao(novaVar, producao.getLadoDir().substring(0, 2)));
                producao.setLadoDir(novaVar + producao.getLadoDir().substring(2));
            }
        }
    }

    // Carrega a GLC do arquivo
    private static void loadGLCFromFile(String filePath, ArrayList<Producao> producoes, ArrayList<Variavel> variaveis, ArrayList<Terminal> terminais) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Arquivo não encontrado: " + filePath);
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                String[] partes = linha.split("->");
                
                // Verifica se a linha contém o delimitador "->"
                if (partes.length != 2) {
                    System.out.println("Formato inválido na linha: " + linha);
                    continue;
                }
                
                String ladoEsq = partes[0].trim();
                String ladoDir = partes[1].trim();
                
                // Adiciona a produção e a variável se o lado esquerdo não estiver vazio
                if (!ladoEsq.isEmpty() && !ladoDir.isEmpty()) {
                    producoes.add(new Producao(ladoEsq, ladoDir));
                    variaveis.add(new Variavel(ladoEsq));
                    for (char simbolo : ladoDir.toCharArray()) {
                        if (Character.isLowerCase(simbolo)) {
                            terminais.add(new Terminal(String.valueOf(simbolo)));
                        }
                    }
                } else {
                    System.out.println("Linha vazia ou inválida encontrada: " + linha);
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao carregar o arquivo: " + e.getMessage());
        }
    }

    // Salva a FNC no arquivo de saída
    private static void saveFNCToFile(String filePath, ArrayList<Producao> producoes) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Producao producao : producoes) {
                writer.write(producao.getLadoEsq() + " -> " + producao.getLadoDir());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Erro ao salvar o arquivo: " + e.getMessage());
        }
    }

    // Gera uma nova variável
    private static String generateNewVariable(ArrayList<Variavel> variaveis) {
        char ultimaLetra = variaveis.isEmpty() ? 'A' : variaveis.get(variaveis.size() - 1).getLadoEsq().charAt(0);
        if (ultimaLetra >= 'Z') {
            throw new RuntimeException("Número máximo de variáveis atingido.");
        }
        String novaVariavel = String.valueOf((char) (ultimaLetra + 1));
        variaveis.add(new Variavel(novaVariavel));
        return novaVariavel;
    }
    

    // Retorna uma variável existente ou cria uma nova para um terminal
    private static String getOrCreateVariable(ArrayList<Variavel> variaveis, char terminal) {
        for (Variavel variavel : variaveis) {
            if (variavel.getLadoEsq().equals(String.valueOf(terminal))) {
                return variavel.getLadoEsq();
            }
        }
        String novaVariavel = generateNewVariable(variaveis);
        return novaVariavel;
    }

    // Ponto de entrada do programa
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Uso: java GLCtoFNC <inputFilePath> <outputFilePath>");
            return;
        }
        convertGLCtoFNC(args[0], args[1]);
    }
}

