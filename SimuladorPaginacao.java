import java.util.Random;
import java.util.HashSet;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class SimuladorPaginacao {

    static final int N = 0;
    static final int I = 1;
    static final int D = 2;
    static final int R = 3;
    static final int M = 4;
    static final int T = 5;

    static final int TAMANHO_RAM = 10;
    static final int TAMANHO_SWAP = 100;
    static final int COLUNAS = 6;
    static final int NUM_INSTRUCOES = 1000;

    static final Random random = new Random();

    public static void main(String[] args) {

        int[][] swap = new int[TAMANHO_SWAP][COLUNAS];
        int[][] ram = new int[TAMANHO_RAM][COLUNAS];

        inicializarSwap(swap);
        inicializarRam(ram, swap);

        System.out.println("--- ESTADO INICIAL ---");
        imprimirMatriz(ram, "RAM INICIAL");
        imprimirMatriz(swap, "SWAP INICIAL");

        String[] algoritmos = {"NRU", "FIFO", "FIFO-SC", "RELOGIO", "LRU", "WS-CLOCK"};

        for (String alg : algoritmos) {
            int[][] ramParaSimulacao = new int[TAMANHO_RAM][COLUNAS];
            int[][] swapParaSimulacao = new int[TAMANHO_SWAP][COLUNAS];

            copiarMatriz(ram, ramParaSimulacao);
            copiarMatriz(swap, swapParaSimulacao);

            System.out.println("\n--- SIMULANDO " + alg + " ---");
            executarSimulacao(alg, ramParaSimulacao, swapParaSimulacao);

            System.out.println("--- ESTADO FINAL (APOS " + alg + ") ---");
            imprimirMatriz(ramParaSimulacao, "RAM FINAL " + alg);
            imprimirMatriz(swapParaSimulacao, "SWAP FINAL " + alg);
        }
    }

    public static void inicializarSwap(int[][] swap) {
        for (int i = 0; i < TAMANHO_SWAP; i++) {
            swap[i][N] = i;
            swap[i][I] = i + 1;
            swap[i][D] = random.nextInt(50) + 1;
            swap[i][R] = 0;
            swap[i][M] = 0;
            swap[i][T] = random.nextInt(9900) + 100;
        }
    }

    public static void inicializarRam(int[][] ram, int[][] swap) {
        HashSet<Integer> indicesSorteados = new HashSet<>();
        while (indicesSorteados.size() < TAMANHO_RAM) {
            indicesSorteados.add(random.nextInt(TAMANHO_SWAP));
        }

        int ramIndex = 0;
        for (int swapIndex : indicesSorteados) {
            System.arraycopy(swap[swapIndex], 0, ram[ramIndex], 0, COLUNAS);
            ramIndex++;
        }
    }

    public static void copiarMatriz(int[][] original, int[][] copia) {
        for (int i = 0; i < original.length; i++) {
            System.arraycopy(original[i], 0, copia[i], 0, original[i].length);
        }
    }

    public static void imprimirMatriz(int[][] matriz, String nome) {
        System.out.println("\n" + nome + ":");
        System.out.println("Linha\t| N\t| I\t| D\t| R\t| M\t| T");
        System.out.println("---------------------------------------------------------");
        String format = "%-7s\t| %-7s\t| %-7s\t| %-7s\t| %-7s\t| %-7s\t| %-7s\n";
        
        for (int i = 0; i < matriz.length; i++) {
             System.out.printf(format, 
                (matriz.length > TAMANHO_RAM ? "L" + i : "F" + i), 
                matriz[i][N], 
                matriz[i][I], 
                matriz[i][D], 
                matriz[i][R], 
                matriz[i][M], 
                matriz[i][T]);
        }
    }

    public static void executarSimulacao(String algoritmo, int[][] ram, int[][] swap) {
        int ponteiroFifo = 0;
        int ponteiroRelogio = 0;

        Queue<Integer> filaFifoSC = new LinkedList<>();
        for (int i = 0; i < TAMANHO_RAM; i++) filaFifoSC.add(i);

        int[] lruTimestamps = new int[TAMANHO_RAM];
        Arrays.fill(lruTimestamps, 0);

        for (int i = 1; i <= NUM_INSTRUCOES; i++) {
            int instrucaoSorteada = random.nextInt(100) + 1;
            int indicePaginaRam = encontrarPaginaRam(ram, instrucaoSorteada);

            if (indicePaginaRam != -1) {
                tratarPageHit(ram[indicePaginaRam]);
                if (algoritmo.equals("LRU")) {
                    lruTimestamps[indicePaginaRam] = i;
                }
            } else {
                int indiceVitima = encontrarIndiceVitima(algoritmo, ram,
                        filaFifoSC, lruTimestamps,
                        ponteiroFifo, ponteiroRelogio);

                tratarPageFault(algoritmo, ram, swap, instrucaoSorteada, i, indiceVitima, lruTimestamps);

                if (algoritmo.equals("FIFO")) {
                    ponteiroFifo = (ponteiroFifo + 1) % TAMANHO_RAM;
                } else if (algoritmo.equals("RELOGIO")) {
                    ponteiroRelogio = (indiceVitima + 1) % TAMANHO_RAM;
                } else if (algoritmo.equals("WS-CLOCK")) {
                    ponteiroRelogio = (indiceVitima + 1) % TAMANHO_RAM;
                }
            }

            if (i % 10 == 0) {
                zerarBitsR(ram);
            }
        }
    }

    public static int encontrarPaginaRam(int[][] ram, int instrucao) {
        for (int i = 0; i < TAMANHO_RAM; i++) {
            if (ram[i][I] == instrucao) {
                return i;
            }
        }
        return -1;
    }

    public static void tratarPageHit(int[] pagina) {
        pagina[R] = 1;
        if (random.nextDouble() < 0.5) {
            pagina[D] += 1;
            pagina[M] = 1;
        }
    }

    public static int encontrarIndiceVitima(String algoritmo, int[][] ram,
                                            Queue<Integer> filaFifoSC, int[] lruTimestamps,
                                            int ponteiroFifo, int ponteiroRelogio) {
        switch (algoritmo) {
            case "NRU":
                return encontrarVitimaNRU(ram);
            case "FIFO":
                return ponteiroFifo;
            case "FIFO-SC":
                return encontrarVitimaFifoSC(ram, filaFifoSC);
            case "RELOGIO":
                return encontrarVitimaRelogio(ram, ponteiroRelogio);
            case "LRU":
                return encontrarVitimaLRU(lruTimestamps);
            case "WS-CLOCK":
                return encontrarVitimaWSClock(ram, ponteiroRelogio);
        }
        return 0;
    }

    public static void tratarPageFault(String algoritmo, int[][] ram, int[][] swap,
                                       int instrucao, int tempoAtual, int indiceVitima,
                                       int[] lruTimestamps) {

        if (ram[indiceVitima][M] == 1) {
            salvarEmSwap(ram[indiceVitima], swap);
        }

        carregarDeSwap(ram, swap, instrucao, indiceVitima);

        if (algoritmo.equals("LRU")) {
            lruTimestamps[indiceVitima] = tempoAtual;
        }
    }

    private static int encontrarVitimaNRU(int[][] ram) {
        int[][] classes = new int[4][TAMANHO_RAM + 1];
        for (int i = 0; i < 4; i++) classes[i][0] = 0;

        for (int i = 0; i < TAMANHO_RAM; i++) {
            int r = ram[i][R];
            int m = ram[i][M];
            int classe = (r * 2) + m;

            int count = ++classes[classe][0];
            classes[classe][count] = i;
        }

        for (int i = 0; i < 4; i++) {
            if (classes[i][0] > 0) {
                int indiceAleatorio = random.nextInt(classes[i][0]) + 1;
                return classes[i][indiceAleatorio];
            }
        }
        return 0;
    }

    private static int encontrarVitimaFifoSC(int[][] ram, Queue<Integer> fila) {
        while (true) {
            int indiceCandidato = fila.poll();
            if (ram[indiceCandidato][R] == 1) {
                ram[indiceCandidato][R] = 0;
                fila.add(indiceCandidato);
            } else {
                fila.add(indiceCandidato);
                return indiceCandidato;
            }
        }
    }

    private static int encontrarVitimaRelogio(int[][] ram, int ponteiro) {
        int ponteiroAtual = ponteiro;
        while (true) {
            if (ram[ponteiroAtual][R] == 0) {
                return ponteiroAtual;
            } else {
                ram[ponteiroAtual][R] = 0;
                ponteiroAtual = (ponteiroAtual + 1) % TAMANHO_RAM;
            }
        }
    }

    private static int encontrarVitimaLRU(int[] lruTimestamps) {
        int indiceMenor = 0;
        int menorTempo = lruTimestamps[0];
        for (int i = 1; i < TAMANHO_RAM; i++) {
            if (lruTimestamps[i] < menorTempo) {
                menorTempo = lruTimestamps[i];
                indiceMenor = i;
            }
        }
        return indiceMenor;
    }

    private static int encontrarVitimaWSClock(int[][] ram, int ponteiro) {
        int ponteiroAtual = ponteiro;
        int maxTentativas = TAMANHO_RAM * 2;
        int tentativas = 0;

        int indiceCandidatoLimpo = -1;

        while (tentativas < maxTentativas) {
            if (ram[ponteiroAtual][R] == 0) {
                int ep = random.nextInt(9900) + 100;
                if (ep > ram[ponteiroAtual][T]) {
                    if (ram[ponteiroAtual][M] == 0) {
                        return ponteiroAtual;
                    } else {
                        if (indiceCandidatoLimpo == -1) {
                            indiceCandidatoLimpo = ponteiroAtual;
                        }
                    }
                }
            } else {
                ram[ponteiroAtual][R] = 0;
            }

            ponteiroAtual = (ponteiroAtual + 1) % TAMANHO_RAM;
            tentativas++;
        }

        if (indiceCandidatoLimpo != -1) {
            return indiceCandidatoLimpo;
        }

        int ponteiroFallback = ponteiro;
         while (true) {
            if (ram[ponteiroFallback][R] == 0 && ram[ponteiroFallback][M] == 0) {
                return ponteiroFallback;
            }
            ponteiroFallback = (ponteiroFallback + 1) % TAMANHO_RAM;
            if(ponteiroFallback == ponteiro) break;
        }
        
        while (true) {
            if (ram[ponteiroFallback][R] == 0) {
                return ponteiroFallback;
            }
            ram[ponteiroFallback][R] = 0;
            ponteiroFallback = (ponteiroFallback + 1) % TAMANHO_RAM;
            if(ponteiroFallback == ponteiro) break;
        }

        return ponteiro;
    }


    public static void salvarEmSwap(int[] paginaRam, int[][] swap) {
        int numeroPagina = paginaRam[N];
        if (numeroPagina >= 0 && numeroPagina < TAMANHO_SWAP) {
            System.arraycopy(paginaRam, 0, swap[numeroPagina], 0, COLUNAS);
            swap[numeroPagina][M] = 0;
        }
    }

    public static void carregarDeSwap(int[][] ram, int[][] swap, int instrucao, int indiceVitima) {
        int indiceSwap = -1;
        for (int i = 0; i < TAMANHO_SWAP; i++) {
            if (swap[i][I] == instrucao) {
                indiceSwap = i;
                break;
            }
        }

        if (indiceSwap != -1) {
            System.arraycopy(swap[indiceSwap], 0, ram[indiceVitima], 0, COLUNAS);
        }
    }

    public static void zerarBitsR(int[][] ram) {
        for (int i = 0; i < TAMANHO_RAM; i++) {
            ram[i][R] = 0;
        }
    }
}