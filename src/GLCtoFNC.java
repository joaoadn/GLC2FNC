import java.io.*;
import java.util.*;

public class GLCtoFNC {

    private static final String START_SYMBOL = "S'";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Uso: java GLCtoFNC <inputFile> <outputFile>");
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];

        try {
            List<String> rules = readGrammar(inputFile);
            rules = processGrammar(rules);
            writeGrammar(outputFile, rules);
        } catch (IOException e) {
            System.err.println("Erro ao processar arquivos: " + e.getMessage());
        }
    }

    private static List<String> readGrammar(String inputFile) throws IOException {
        List<String> rules = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                rules.add(line.trim());
            }
        }
        return rules;
    }

    private static void writeGrammar(String outputFile, List<String> rules) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            for (String rule : rules) {
                bw.write(rule);
                bw.newLine();
            }
        }
    }

    private static List<String> processGrammar(List<String> rules) {
        rules = removeInitialRecursion(rules);
        rules = removeLambdaRules(rules);
        rules = removeChainRules(rules);
        rules = removeNonGeneratingRules(rules);
        rules = removeUnreachableSymbols(rules);
        rules = replaceTerminalsWithVariables(rules);
        return rules;
    }
    

    private static List<String> removeInitialRecursion(List<String> rules) {
        List<String> newRules = new ArrayList<>();
        for (String rule : rules) {
            if (rule.startsWith("S ->")) {
                newRules.add(START_SYMBOL + " -> S");
                String[] productions = rule.substring(4).split("\\|");
                List<String> updatedProductions = new ArrayList<>();
                for (String production : productions) {
                    production = production.trim();
                    if (!production.isEmpty() && !production.equals("S")) {
                        updatedProductions.add(production);
                    }
                }
                newRules.add("S -> " + String.join(" | ", updatedProductions));
            } else {
                newRules.add(rule);
            }
        }
        return newRules;
    }

    private static List<String> removeLambdaRules(List<String> rules) {
        Set<String> nullableVariables = identifyNullableVariables(rules);
        List<String> newRules = new ArrayList<>();
        Map<String, Set<String>> productions = new HashMap<>();
        boolean hasLambdaProductionForStartSymbol = false;

        for (String rule : rules) {
            if (rule.contains(" -> ")) {
                String[] parts = rule.split(" -> ");
                String variable = parts[0].trim();
                String[] prods = parts[1].split("\\|");
                Set<String> prodSet = productions.computeIfAbsent(variable, k -> new HashSet<>());

                for (String prod : prods) {
                    prod = prod.trim();
                    if (!prod.equals(".")) {
                        prodSet.add(prod);
                    } else if (variable.equals("S")) {
                        hasLambdaProductionForStartSymbol = true;
                    }
                }
            }
        }

        for (Map.Entry<String, Set<String>> entry : productions.entrySet()) {
            String variable = entry.getKey();
            Set<String> prodSet = entry.getValue();
            Set<String> newProductions = new HashSet<>(prodSet);
            for (String prod : prodSet) {
                generateCombinations(newProductions, prod, nullableVariables);
            }
            newProductions.removeIf(String::isEmpty);
            newRules.add(variable + " -> " + String.join(" | ", newProductions));
        }

        if (nullableVariables.contains("S") || hasLambdaProductionForStartSymbol) {
            newRules.removeIf(rule -> rule.startsWith("S' ->"));
            newRules.add("S' -> S | .");
        }

        return newRules;
    }

    private static Set<String> identifyNullableVariables(List<String> rules) {
        Set<String> nullableVariables = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (String rule : rules) {
                if (rule.contains(" -> ")) {
                    String[] parts = rule.split(" -> ");
                    String variable = parts[0].trim();
                    String[] prods = parts[1].split("\\|");
                    for (String prod : prods) {
                        boolean allNullable = true;
                        for (char c : prod.trim().toCharArray()) {
                            if (Character.isUpperCase(c) && !nullableVariables.contains(String.valueOf(c))) {
                                allNullable = false;
                                break;
                            }
                        }
                        if (allNullable || prod.trim().equals(".")) {
                            if (nullableVariables.add(variable)) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        } while (changed);
        return nullableVariables;
    }

    private static void generateCombinations(Set<String> newProductions, String production, Set<String> nullableVariables) {
        char[] chars = production.toCharArray();
        int n = chars.length;
        for (int i = 0; i < (1 << n); i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) == 0 || !nullableVariables.contains(String.valueOf(chars[j]))) {
                    sb.append(chars[j]);
                }
            }
            String newProd = sb.toString();
            if (!newProd.isEmpty()) {
                newProductions.add(newProd);
            }
        }
    }

    private static List<String> removeChainRules(List<String> rules) {
        Map<String, Set<String>> productions = new HashMap<>();
        for (String rule : rules) {
            if (rule.contains(" -> ")) {
                String[] parts = rule.split(" -> ");
                String variable = parts[0].trim();
                String[] prods = parts[1].split("\\|");
                Set<String> prodSet = productions.computeIfAbsent(variable, k -> new HashSet<>());
                for (String prod : prods) {
                    prodSet.add(prod.trim());
                }
            }
        }

        boolean changed;
        do {
            changed = false;
            Map<String, Set<String>> newProductions = new HashMap<>(productions);
            for (Map.Entry<String, Set<String>> entry : productions.entrySet()) {
                String variable = entry.getKey();
                Set<String> prodSet = entry.getValue();
                Set<String> updatedProductions = new HashSet<>();
                for (String prod : prodSet) {
                    if (Character.isUpperCase(prod.charAt(0)) && productions.containsKey(prod)) {
                        updatedProductions.addAll(productions.get(prod));
                        changed = true;
                    } else {
                        updatedProductions.add(prod);
                    }
                }
                if (!newProductions.get(variable).equals(updatedProductions)) {
                    newProductions.put(variable, updatedProductions);
                }
            }
            productions = newProductions;
        } while (changed);

        Set<String> updatedRules = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : productions.entrySet()) {
            String variable = entry.getKey();
            Set<String> prodSet = entry.getValue();
            if (!prodSet.isEmpty()) {
                updatedRules.add(variable + " -> " + String.join(" | ", prodSet));
            }
        }
        return new ArrayList<>(updatedRules);
    }

    private static List<String> removeNonGeneratingRules(List<String> rules) {
        Map<String, Set<String>> productions = new HashMap<>();
        for (String rule : rules) {
            if (rule.contains(" -> ")) {
                String[] parts = rule.split(" -> ");
                String variable = parts[0].trim();
                String[] prods = parts[1].split("\\|");
                Set<String> prodSet = productions.computeIfAbsent(variable, k -> new HashSet<>());
                for (String prod : prods) {
                    prodSet.add(prod.trim());
                }
            }
        }

        Set<String> generatingVariables = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, Set<String>> entry : productions.entrySet()) {
                String variable = entry.getKey();
                Set<String> prodSet = entry.getValue();
                for (String prod : prodSet) {
                    if (prod.chars().allMatch(c -> Character.isLowerCase(c) || generatingVariables.contains(String.valueOf((char) c)))) {
                        if (generatingVariables.add(variable)) {
                            changed = true;
                        }
                    }
                }
            }
        } while (changed);

        List<String> validRules = new ArrayList<>();
        for (String rule : rules) {
            if (rule.contains(" -> ")) {
                String[] parts = rule.split(" -> ");
                String variable = parts[0].trim();
                if (generatingVariables.contains(variable)) {
                    validRules.add(rule);
                }
            }
        }
        return validRules;
    }

    private static List<String> removeUnreachableSymbols(List<String> rules) {
        Set<String> reachableVariables = new HashSet<>();
        reachableVariables.add(START_SYMBOL);
        boolean changed;
        do {
            changed = false;
            Set<String> newReachableVariables = new HashSet<>(reachableVariables);
            for (String rule : rules) {
                if (rule.contains(" -> ")) {
                    String[] parts = rule.split(" -> ");
                    String variable = parts[0].trim();
                    String[] prods = parts[1].split("\\|");
                    if (reachableVariables.contains(variable)) {
                        for (String prod : prods) {
                            for (char c : prod.trim().toCharArray()) {
                                if (Character.isUpperCase(c)) {
                                    newReachableVariables.add(String.valueOf(c));
                                }
                            }
                        }
                        if (!newReachableVariables.equals(reachableVariables)) {
                            reachableVariables = new HashSet<>(newReachableVariables);
                            changed = true;
                        }
                    }
                }
            }
        } while (changed);

        List<String> validRules = new ArrayList<>();
        for (String rule : rules) {
            if (rule.contains(" -> ")) {
                String[] parts = rule.split(" -> ");
                String variable = parts[0].trim();
                if (reachableVariables.contains(variable)) {
                    validRules.add(rule);
                }
            }
        }
        return validRules;
    }

    private static List<String> replaceTerminalsWithVariables(List<String> rules) {
        Map<String, String> terminalToVariable = new HashMap<>();
        Set<String> usedVariables = new HashSet<>();
        List<String> terminalRules = new ArrayList<>();
        List<String> updatedRules = new ArrayList<>();
        Set<String> terminalsToReplace = new HashSet<>();
    
        // Primeiro, identificar quais terminais devem ser substituídos
        for (String rule : rules) {
            if (rule.contains(" -> ")) {
                String[] parts = rule.split(" -> ");
                String[] prods = parts[1].split("\\|");
                for (String prod : prods) {
                    String trimmedProd = prod.trim();
                    if (trimmedProd.length() > 1) {
                        for (char c : trimmedProd.toCharArray()) {
                            if (Character.isLowerCase(c)) {
                                terminalsToReplace.add(String.valueOf(c));
                            }
                        }
                    }
                }
            }
        }
    
        // Criar variáveis para terminais e adicionar suas regras
        for (char c = 'a'; c <= 'z'; c++) {
            String terminal = String.valueOf(c);
            if (terminalsToReplace.contains(terminal)) {
                String variable = String.valueOf(Character.toUpperCase(c));
                if (!usedVariables.contains(variable)) {
                    terminalToVariable.put(terminal, variable);
                    usedVariables.add(variable);
                    terminalRules.add(variable + " -> " + terminal);
                }
            }
        }
    
        // Atualizar regras substituindo terminais por variáveis
        for (String rule : rules) {
            if (rule.contains(" -> ")) {
                String[] parts = rule.split(" -> ");
                String variable = parts[0].trim();
                String[] prods = parts[1].split("\\|");
                Set<String> updatedProds = new HashSet<>();
                for (String prod : prods) {
                    String trimmedProd = prod.trim();
                    if (trimmedProd.length() == 1 && Character.isLowerCase(trimmedProd.charAt(0))) {
                        // Se a produção é um terminal de tamanho 1, mantê-lo inalterado
                        updatedProds.add(trimmedProd);
                    } else {
                        // Substituir terminais maiores por variáveis
                        StringBuilder newProd = new StringBuilder();
                        for (char c : trimmedProd.toCharArray()) {
                            String charStr = String.valueOf(c);
                            newProd.append(Character.isLowerCase(c) && terminalToVariable.containsKey(charStr) ? terminalToVariable.get(charStr) : charStr);
                        }
                        if (newProd.length() > 0) {
                            updatedProds.add(newProd.toString());
                        }
                    }
                }
                updatedRules.add(variable + " -> " + String.join(" | ", updatedProds));
            } else {
                updatedRules.add(rule);
            }
        }
    
        updatedRules.addAll(terminalRules);
        return updatedRules;
    }

    private static List<String> convertToBinaryRules(List<String> rules) {
        Map<String, String> variableMap = new HashMap<>();
        List<String> updatedRules = new ArrayList<>();
        List<String> additionalRules = new ArrayList<>();
        int variableCounter = 1;

        // Passo 1: Adicionar regras originais e identificar variáveis auxiliares
        for (String rule : rules) {
            if (rule.contains(" -> ")) {
                String[] parts = rule.split(" -> ");
                String variable = parts[0].trim();
                String[] prods = parts[1].split("\\|");

                for (String prod : prods) {
                    prod = prod.trim();
                    if (prod.length() > 2) {
                        // Criar variáveis auxiliares para regras de comprimento maior
                        String newVariable = "T" + variableCounter++;
                        variableMap.put(newVariable, prod);
                        additionalRules.add(newVariable + " -> " + prod.substring(0, 2));
                        for (int i = 2; i < prod.length(); i++) {
                            newVariable = "T" + variableCounter++;
                            additionalRules.add(newVariable + " -> " + prod.substring(i, i + 2));
                        }
                    }
                }
            }
        }

        // Passo 2: Atualizar regras para usar variáveis auxiliares
        Map<String, String> terminalToVariable = new HashMap<>();
        for (String rule : rules) {
            if (rule.contains(" -> ")) {
                String[] parts = rule.split(" -> ");
                String variable = parts[0].trim();
                String[] prods = parts[1].split("\\|");
                Set<String> updatedProds = new HashSet<>();

                for (String prod : prods) {
                    String trimmedProd = prod.trim();
                    if (trimmedProd.length() == 1 && Character.isLowerCase(trimmedProd.charAt(0))) {
                        // Se a produção é um terminal de tamanho 1, mantê-lo inalterado
                        updatedProds.add(trimmedProd);
                    } else {
                        // Substituir terminais maiores por variáveis
                        StringBuilder newProd = new StringBuilder();
                        String[] tokens = trimmedProd.split("(?<=\\G..)");
                        for (String token : tokens) {
                            String newVar = variableMap.get(token);
                            if (newVar != null) {
                                newProd.append(newVar).append(" ");
                            } else {
                                newProd.append(token).append(" ");
                            }
                        }
                        if (newProd.length() > 0) {
                            updatedProds.add(newProd.toString().trim());
                        }
                    }
                }
                updatedRules.add(variable + " -> " + String.join(" | ", updatedProds));
            } else {
                updatedRules.add(rule);
            }
        }

        // Adicionar regras auxiliares
        updatedRules.addAll(additionalRules);

        // Remover possíveis regras redundantes ou duplicadas
        Set<String> uniqueRules = new HashSet<>(updatedRules);
        return new ArrayList<>(uniqueRules);
    }
    
}
    















































































