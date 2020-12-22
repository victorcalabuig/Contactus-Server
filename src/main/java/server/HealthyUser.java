package server;

import java.sql.ResultSet;
import java.sql.SQLException;

public class HealthyUser extends User {

    public HealthyUser(ResultSet rs) throws SQLException {
        userId = rs.getInt(1);
        username = rs.getString(2);
    }

}
