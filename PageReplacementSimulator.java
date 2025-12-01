import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PageReplacementSimulator {

    // Definição das colunas da Matriz
    private static final int COL_N = 0; // Número da Página
    private static final int COL_I = 1; // Instrução
    private static final int COL_D = 2; // Dado
    private static final int COL_R = 3; // Bit de Acesso
    private static final int COL_M = 4; // Bit de Modificação
    private static final int COL_T = 5; // Tempo de Envelhecimento

    private static final int RAM_SIZE = 10;
    private static final int SWAP_SIZE = 100;
    private static final int NUM_INSTRUCOES = 1000;

    private static int[][] ram = new int[RAM_SIZE][6];
    private static int[][] swap = new int[SWAP_SIZE][6];
    
    // Auxiliares para os algoritmos
    private static int fifoPointer = 0; // Para FIFO e FIFO-SC
    private static int clockPointer = 0; // Para Relógio e WS-Clock
    private static long[] lruCounters = new long[RAM_SIZE]; // Para LRU (contador de acesso)
    private static long instructionGlobalCounter = 0; // Tempo global simulado

    private static Random random = new Random();

    public static void main(String[] args) {
        String[] algoritmos = {"NRU", "FIFO", "FIFO-SC", "RELOGIO", "LRU", "WS-CLOCK"};

        System.out.println("=== INÍCIO DA SIMULACAO GERAL ===");

        for (String algo : algoritmos) {
            System.out.println("\n##########################################################");
            System.out.println("EXECUTANDO ALGORITMO: " + algo);
            System.out.println("##########################################################");
 
            inicializarMatrizes();

            System.out.println("\n--- ESTADO INICIAL (RAM) ---");
            imprimirMatriz(ram, "RAM");

            fifoPointer = 0;
            clockPointer = 0;
            instructionGlobalCounter = 0;
            Arrays.fill(lruCounters, 0);


            rodarSimulador(algo);

            System.out.println("\n--- ESTADO FINAL (RAM) - " + algo + " ---");
            imprimirMatriz(ram, "RAM");
            System.out.println("\n--- ESTADO FINAL (SWAP) - " + algo + " ---");
            imprimirMatriz(swap, "SWAP");
        }
    }


    private static void inicializarMatrizes() {
        
        for (int i = 0; i < SWAP_SIZE; i++) {
            swap[i][COL_N] = i;  
            swap[i][COL_I] = i + 1; 
            swap[i][COL_D] = random.nextInt(50) + 1; 
            swap[i][COL_R] = 0;
            swap[i][COL_M] = 0;
            swap[i][COL_T] = random.nextInt(9900) + 100; 
        }

        List<Integer> indicesSwapDisponiveis = new ArrayList<>();
        for (int i = 0; i < SWAP_SIZE; i++) indicesSwapDisponiveis.add(i);

        for (int i = 0; i < RAM_SIZE; i++) {
            int indexSorteado = random.nextInt(indicesSwapDisponiveis.size());
            int linhaSwap = indicesSwapDisponiveis.remove(indexSorteado);
            
            System.arraycopy(swap[linhaSwap], 0, ram[i], 0, 6);
        }
    }


    private static void rodarSimulador(String algoritmo) {
        int pageFaults = 0;

        for (int instrucaoAtual = 1; instrucaoAtual <= NUM_INSTRUCOES; instrucaoAtual++) {
            instructionGlobalCounter++;

            if (instrucaoAtual % 10 == 0) {
                zerarBitR();
            }
            int instrucaoRequerida = random.nextInt(100) + 1;

            int indiceMemoria = buscarNaRam(instrucaoRequerida);

            if (indiceMemoria != -1) {
                executarAcoesHit(indiceMemoria);
            } else {
                pageFaults++;
                
                int indiceVitima = executarAlgoritmoSubstituicao(algoritmo);

                realizarSwapOut(indiceVitima);

                realizarSwapIn(indiceVitima, instrucaoRequerida);
            }
        }
        System.out.println("Total de Page Faults: " + pageFaults);
    }

    private static int buscarNaRam(int instrucao) {
        for (int i = 0; i < RAM_SIZE; i++) {
            if (ram[i][COL_I] == instrucao) {
                return i;
            }
        }
        return -1;
    }

    private static void executarAcoesHit(int index) {
        ram[index][COL_R] = 1;
        lruCounters[index] = instructionGlobalCounter;

        if (random.nextBoolean()) {
            ram[index][COL_D] = ram[index][COL_D] + 1;
            ram[index][COL_M] = 1;
        }
    }

    private static void realizarSwapOut(int indexRam) {
        if (ram[indexRam][COL_M] == 1) {
            int numPagina = ram[indexRam][COL_N];
            for (int k = 0; k < SWAP_SIZE; k++) {
                if (swap[k][COL_N] == numPagina) {
                    swap[k][COL_D] = ram[indexRam][COL_D]; 
                    swap[k][COL_M] = 0; 
                    break;
                }
            }
        }
    }

    private static void realizarSwapIn(int indexRam, int instrucaoRequerida) {

        int indexSwap = -1;
        for (int k = 0; k < SWAP_SIZE; k++) {
            if (swap[k][COL_I] == instrucaoRequerida) {
                indexSwap = k;
                break;
            }
        }

        if (indexSwap != -1) {

            System.arraycopy(swap[indexSwap], 0, ram[indexRam], 0, 6);
            
            // Ao carregar:
            ram[indexRam][COL_R] = 1; 
            ram[indexRam][COL_M] = 0; 
            lruCounters[indexRam] = instructionGlobalCounter; 
        } else {
            System.err.println("Erro Crítico: Instrução " + instrucaoRequerida + " não encontrada no SWAP!");
        }
    }

    private static void zerarBitR() {
        for (int i = 0; i < RAM_SIZE; i++) {
            ram[i][COL_R] = 0;
        }
    }


    private static int executarAlgoritmoSubstituicao(String algoritmo) {
        switch (algoritmo) {
            case "NRU": return algoritmoNRU();
            case "FIFO": return algoritmoFIFO();
            case "FIFO-SC": return algoritmoFIFOSC();
            case "RELOGIO": return algoritmoRelogio();
            case "LRU": return algoritmoLRU();
            case "WS-CLOCK": return algoritmoWSClock();
            default: return 0;
        }
    }

    private static int algoritmoNRU() {

        List<Integer> classe0 = new ArrayList<>();
        List<Integer> classe1 = new ArrayList<>();
        List<Integer> classe2 = new ArrayList<>();
        List<Integer> classe3 = new ArrayList<>();

        for (int i = 0; i < RAM_SIZE; i++) {
            int r = ram[i][COL_R];
            int m = ram[i][COL_M];
            if (r == 0 && m == 0) classe0.add(i);
            else if (r == 0 && m == 1) classe1.add(i);
            else if (r == 1 && m == 0) classe2.add(i);
            else classe3.add(i);
        }

        if (!classe0.isEmpty()) return classe0.get(random.nextInt(classe0.size()));
        if (!classe1.isEmpty()) return classe1.get(random.nextInt(classe1.size()));
        if (!classe2.isEmpty()) return classe2.get(random.nextInt(classe2.size()));
        return classe3.get(random.nextInt(classe3.size()));
    }

    private static int algoritmoFIFO() {
        int vitima = fifoPointer;
        fifoPointer = (fifoPointer + 1) % RAM_SIZE;
        return vitima;
    }

    private static int algoritmoFIFOSC() {
        while (true) {
            int r = ram[fifoPointer][COL_R];
            if (r == 0) {
                int vitima = fifoPointer;
                fifoPointer = (fifoPointer + 1) % RAM_SIZE;
                return vitima;
            } else {
                ram[fifoPointer][COL_R] = 0;
                fifoPointer = (fifoPointer + 1) % RAM_SIZE;
            }
        }
    }

    private static int algoritmoRelogio() {
        while (true) {
            int r = ram[clockPointer][COL_R];
            if (r == 0) {
                int vitima = clockPointer;
                clockPointer = (clockPointer + 1) % RAM_SIZE;
                return vitima;
            } else {
                ram[clockPointer][COL_R] = 0; 
                clockPointer = (clockPointer + 1) % RAM_SIZE;
            }
        }
    }

    private static int algoritmoLRU() {
        long minTime = Long.MAX_VALUE;
        int indexLRU = 0;

        for (int i = 0; i < RAM_SIZE; i++) {
            if (lruCounters[i] < minTime) {
                minTime = lruCounters[i];
                indexLRU = i;
            }
        }
        return indexLRU;
    }

    private static int algoritmoWSClock() {
        int tentativas = 0;

        while (tentativas < RAM_SIZE * 2) {
            
            int r = ram[clockPointer][COL_R];
            int t = ram[clockPointer][COL_T]; 
            
            if (r == 1) {
                ram[clockPointer][COL_R] = 0;

            } else {

                int ep = random.nextInt(9900) + 100; 

                if (ep > t) {
                    int vitima = clockPointer;
                    clockPointer = (clockPointer + 1) % RAM_SIZE;
                    return vitima;
                }
            }
            
            clockPointer = (clockPointer + 1) % RAM_SIZE;
            tentativas++;
        }

        int vitima = clockPointer;
        clockPointer = (clockPointer + 1) % RAM_SIZE;
        return vitima;
    }

    private static void imprimirMatriz(int[][] matriz, String titulo) {
        System.out.println("Tabela: " + titulo);
        System.out.printf("%-5s %-5s %-5s %-5s %-5s %-5s%n", "N", "I", "D", "R", "M", "T");
        System.out.println("----------------------------------------");
        
        // Se for SWAP, imprime apenas os 10 primeiros e 10 últimos para não poluir, 
        // ou imprime tudo se desejar. Aqui imprimirei resumido se for muito grande.
        int linhas = matriz.length;
        
        if (linhas > 20) {
            for (int i = 0; i < 10; i++) imprimirLinha(matriz[i]);
            System.out.println("... (ocultando " + (linhas - 20) + " linhas) ...");
            for (int i = linhas - 10; i < linhas; i++) imprimirLinha(matriz[i]);
        } else {
            for (int i = 0; i < linhas; i++) {
                imprimirLinha(matriz[i]);
            }
        }
        System.out.println("----------------------------------------");
    }

    private static void imprimirLinha(int[] linha) {
        System.out.printf("%-5d %-5d %-5d %-5d %-5d %-5d%n", 
            linha[COL_N], linha[COL_I], linha[COL_D], 
            linha[COL_R], linha[COL_M], linha[COL_T]);
    }
}