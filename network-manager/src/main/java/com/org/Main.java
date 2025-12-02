package com.org;

import com.org.db.PostgresPool;
import com.org.snmp.SnmpScanner;
import com.org.web.HttpServer;

public class Main {

    public static void main(String[] args) {
        try {
            // 1 Inicializa pool do banco
            PostgresPool.init("localhost", 5432, "ogmr", "postgres", "udesc");
            System.out.println("Pool do banco inicializada.");

            // Scans periódicos do SNMMP via thread
            
            Thread scannerThread = new Thread(() -> {
                SnmpScanner scanner = new SnmpScanner();
                while (true) {
                    try {
                        scanner.scanAllSwitches();
                        Thread.sleep(60000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            scannerThread.start();

            // 2️ Inicializa o servidor HTTP em uma thread separada
            HttpServer server = new HttpServer(8080);
            Thread serverThread = new Thread(server::start);
            serverThread.start();
            System.out.println("Servidor HTTP iniciado na porta 8080.");

            // O main thread continua vivo para manter listener e servidor ativos
            serverThread.join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
