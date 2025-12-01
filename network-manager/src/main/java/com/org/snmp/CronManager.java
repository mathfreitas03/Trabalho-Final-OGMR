package com.org.snmp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.*;

import com.org.db.PostgresPool;

// Código provisório apenas para testes em ambientes Windows (a classe não utiliza o crontab do Linux).

public class CronManager {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    /**
     * Bloqueia portas imediatamente e agenda desbloqueio.
     * @param portIds Lista de IDs de portas a bloquear
     * @param durationSeconds Tempo em segundos para desbloqueio
     */
    public static void scheduleBlock(List<Integer> portIds, int durationSeconds) {
        // 1. Bloqueia imediatamente
        updatePortsStatus(portIds, false);

        // 2. Agenda desbloqueio
        scheduler.schedule(() -> {
            updatePortsStatus(portIds, true);
        }, durationSeconds, TimeUnit.SECONDS);

        System.out.println("Bloqueio agendado para portas " + portIds + " por " + durationSeconds + " segundos.");
    }

    /**
     * Atualiza status das portas no banco
     * @param portIds IDs das portas
     * @param status true = ativa, false = bloqueada
     */
    private static void updatePortsStatus(List<Integer> portIds, boolean status) {
        try (Connection conn = PostgresPool.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "update port set status = ? where id = ?")) {

                for (Integer portId : portIds) {
                    ps.setBoolean(1, status);
                    ps.setInt(2, portId);
                    ps.addBatch();
                }

                ps.executeBatch();
                conn.commit();
                System.out.println("Status portas atualizadas: " + portIds + " -> " + (status ? "Ativas" : "Bloqueadas"));
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Desbloqueia portas imediatamente (para caso o Node queira cancelar)
     * @param portIds IDs das portas
     */
    public static void unblockNow(List<Integer> portIds) {
        updatePortsStatus(portIds, true);
    }

    /**
     * Encerra o scheduler ao desligar o servidor
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
