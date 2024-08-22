import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class GLCtoFNC {

    public static void main(String[] args) {
        // Verifica se o número correto de argumentos foi passado
        if (args.length != 3) {
            System.out.println("Uso: java GLCtoFNC <inputFilePath> <variaveisFilePath> <outputFilePath>");
            return;
        }
        // Converte a GLC para FNC usando os arquivos especificados
        convertGLCtoFNC(args[0], args[1], args[2]);
    }

    public static void convertGLCtoFNC(String inputFilePath, String variaveisFilePath, String outputFilePath) {
        // Estruturas de dados para armazenar as produções, variáveis e terminais
        ArrayList<Producao> producoes = new ArrayList<>();
        ArrayList<String> variaveis = new ArrayList<>();
        ArrayList<String> terminais = new ArrayList<>();
        Map<String, String> terminalToVar = new HashMap<>();
        Map<String, String> varToNewVar = new HashMap<>();

        // Carrega a GLC do arquivo de entrada
        loadGLCFromFile(inputFilePath, producoes, variaveis, terminais);

        // Remove produções vazias (lambda-produções)
        removeEmptyProductions(producoes);

        // Substitui terminais por variáveis
        replaceTerminalsWithVariables(producoes, terminalToVar, variaveis);

        // Garante que cada produção tenha no máximo duas variáveis no lado direito
        ensureTwoVariablesPerProduction(producoes, varToNewVar, variaveis);

        // Salva a FNC no arquivo de saída
        saveFNCToFile(outputFilePath, producoes);
    }

    // Remove produções vazias (lambda-produções)
    private static void removeEmptyProductions(ArrayList<Producao> producoes) {
        for (int i = 0; i < producoes.size(); i++) {
            Producao producao = producoes.get(i);
            if (producao.getLadoDir().contains(".")) {
                String variavel = producao.getLadoEsq();
                producoes.remove(i);
                i--;
                for (Producao p : producoes) {
                    if (p.getLadoDir().contains(variavel)) {
                        p.setLadoDir(p.getLadoDir().replace(variavel, ""));
                    }
                }
            }
        }
    }

    // Substitui terminais por variáveis
    private static void replaceTerminalsWithVariables(ArrayList<Producao> producoes, Map<String, String> terminalToVar, ArrayList<String> variaveis) {
        for (Producao producao : producoes) {
            StringBuilder novoLadoDir = new StringBuilder();
            for (char simbolo : producao.getLadoDir().toCharArray()) {
                if (Character.isLowerCase(simbolo)) {
                    String terminal = String.valueOf(simbolo);
                    if (!terminalToVar.containsKey(terminal)) {
                        String novaVar = generateNewVariable(variaveis);
                        terminalToVar.put(terminal, novaVar);
                        variaveis.add(novaVar);
                    }
                    novoLadoDir.append(terminalToVar.get(terminal));
                } else {
                    novoLadoDir.append(simbolo);
                }
            }
            producao.setLadoDir(novoLadoDir.toString());
        }
    }

    // Garante que cada produção tenha no máximo duas variáveis no lado direito
    private static void ensureTwoVariablesPerProduction(ArrayList<Producao> producoes, Map<String, String> varToNewVar, ArrayList<String> variaveis) {
        for (int i = 0; i < producoes.size(); i++) {
            Producao producao = producoes.get(i);
            String ladoDir = producao.getLadoDir();
            while (ladoDir.length() > 2) {
                String prefixo = ladoDir.substring(0, 2);
                ladoDir = ladoDir.substring(2);

                // Cria uma nova variável se ainda não existir para o prefixo
                if (!varToNewVar.containsKey(prefixo)) {
                    String novaVar = generateNewVariable(variaveis);
                    varToNewVar.put(prefixo, novaVar);
                    producoes.add(new Producao(novaVar, prefixo));
                }
                ladoDir = varToNewVar.get(prefixo) + ladoDir;
            }
            producao.setLadoDir(ladoDir);
        }
    }

    // Carrega a GLC do arquivo
    private static void loadGLCFromFile(String filePath, ArrayList<Producao> producoes, ArrayList<String> variaveis, ArrayList<String> terminais) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty()) continue;

                // Divide a linha em parte esquerda e parte direita da produção
                String[] partes = linha.split("->");
                if (partes.length != 2) continue;

                String ladoEsq = partes[0].trim();
                String[] regras = partes[1].trim().split("\\|");

                // Adiciona a variável se ainda não estiver na lista
                if (!variaveis.contains(ladoEsq)) variaveis.add(ladoEsq);

                for (String regra : regras) {
                    String regraTrimmed = regra.trim();
                    if (!regraTrimmed.isEmpty()) {
                        producoes.add(new Producao(ladoEsq, regraTrimmed));
                    }
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

    // Gera uma nova variável (T1, T2, etc.)
    private static String generateNewVariable(ArrayList<String> variaveis) {
        int index = 1;
        String novaVariavel;
        do {
            novaVariavel = "T" + index++;
        } while (variaveis.contains(novaVariavel));
        return novaVariavel;
    }
}




