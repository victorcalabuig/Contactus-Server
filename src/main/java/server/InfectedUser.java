package server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class InfectedUser extends User {

    public String infectedSince;

    public InfectedUser(ResultSet rs) throws SQLException {
        userId = rs.getInt(1);
        username = rs.getString(2);
        infectedSince = rs.getTimestamp(3).toString();
    }

    public String toString(){
        return String.format("%d#%s#%s", userId, username, infectedSince);
    }

}
