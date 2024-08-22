// Classe para representar uma produção na GLC
class Producao {
    private String ladoEsq;
    private String ladoDir;

    public Producao(String ladoEsq, String ladoDir) {
        this.ladoEsq = ladoEsq;
        this.ladoDir = ladoDir;
    }

    public String getLadoEsq() {
        return ladoEsq;
    }

    public String getLadoDir() {
        return ladoDir;
    }

    public void setLadoDir(String ladoDir) {
        this.ladoDir = ladoDir;
    }

    @Override
    public String toString() {
        return ladoEsq + " -> " + ladoDir;
    }
}
