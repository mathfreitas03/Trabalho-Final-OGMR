package db;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class TrapEventDAO {

    public void saveTrapEvent(int switchId, int portNumber, String eventType) {
        try (Connection conn = Database.getConnection()) {

            String sql = "INSERT INTO trap_event (switch_id, port_number, event_type) VALUES (?, ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, switchId);
            stmt.setInt(2, portNumber);
            stmt.setString(3, eventType);

            stmt.executeUpdate();

        } catch (Exception e) {
            System.err.println("Erro TrapEventDAO: " + e.getMessage());
        }
    }
}
