package db;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class PortDAO {

    public void updatePortStatus(int switchId, int portNumber, boolean active) {
        try (Connection conn = Database.getConnection()) {

            String query =
                "INSERT INTO port (switch_id, number, status) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT (switch_id, number) DO UPDATE SET status = EXCLUDED.status";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, switchId);
            stmt.setInt(2, portNumber);
            stmt.setBoolean(3, active);
            stmt.executeUpdate();

        } catch (Exception e) {
            System.err.println("Erro PortDAO.updatePortStatus: " + e.getMessage());
        }
    }
}
