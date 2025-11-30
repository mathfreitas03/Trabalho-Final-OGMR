package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SwitchDAO {

    public Integer findOrCreateSwitch(String hostname, String ipv4) {
        try (Connection conn = Database.getConnection()) {

            // verifica se já existe
            String query = "SELECT id FROM switch WHERE ipv4 = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, ipv4);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }

            // se não existir, cria
            String insert = "INSERT INTO switch (hostname, ipv4) VALUES (?, ?) RETURNING id";
            PreparedStatement stmtInsert = conn.prepareStatement(insert);
            stmtInsert.setString(1, hostname);
            stmtInsert.setString(2, ipv4);

            ResultSet rs2 = stmtInsert.executeQuery();
            if (rs2.next()) {
                return rs2.getInt(1);
            }

        } catch (Exception e) {
            System.err.println("Erro SwitchDAO: " + e.getMessage());
        }
        return null;
    }
}
