package server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SuspectUser extends User {

    public String suspectSince;
    public String closeContact;
    public String contactDuration;

    public SuspectUser(ResultSet rs) throws SQLException {
        userId = rs.getInt(1);
        username = rs.getString(2);
        suspectSince = rs.getTimestamp(3).toString();
        closeContact = rs.getString(4);
        contactDuration = millisToTime(rs.getTimestamp(5));
    }

    private String millisToTime(Timestamp ts){
        Date date = new Date(ts.getTime());
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(date);
    }

    public String toString(){
        return String.format("%d#%s#%s#%s#%s", userId, username, suspectSince, closeContact, contactDuration);
    }

}