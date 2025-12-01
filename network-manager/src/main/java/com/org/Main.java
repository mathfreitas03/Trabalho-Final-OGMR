package com.org;

import com.org.db.PostgresPool;
import com.org.snmp.SnmpTrapListener;
import com.org.web.HttpServer;

public class Main {

    public static void main(String[] args) {
        try {
            // 1 Inicializa pool do banco
            PostgresPool.init("localhost", 5432, "ogmr", "postgres", "udesc");
            System.out.println("Pool do banco inicializada.");

            // 2️ Inicializa o servidor HTTP em uma thread separada
            HttpServer server = new HttpServer(8080);
            Thread serverThread = new Thread(server::start);
            serverThread.start();
            System.out.println("Servidor HTTP iniciado na porta 8080.");

            // 3 Inicializa o listener SNMP (porta 162)
            SnmpTrapListener trapListener = new SnmpTrapListener(162);
            System.out.println("Listener SNMP ativo na porta 162.");

            // O main thread continua vivo para manter listener e servidor ativos
            serverThread.join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
