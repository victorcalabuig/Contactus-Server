package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Clase para crear la base de datos del servidor y crear las tablas
 * necesarias (de momento User y Location).
 */
public class DatabaseCreation {
    
    static String userSQLCreate = "CREATE TABLE User ("
        + "userId INTEGER PRIMARY KEY, "
        + "username TEXT UNIQUE NOT NULL, "
        + "email TEXT UNIQUE, "
        + "password TEXT NOT NULL, "
        + "state INT DEFAULT 0, "  //0 = healthy; 1 = infected; null = suspect (improve some ops)
        + "lastLocation INTEGER UNIQUE, "
        + "FOREIGN KEY (lastLocation) REFERENCES Location (locationId) ON UPDATE RESTRICT ON DELETE RESTRICT)";

    static String adminSQLCreate = "CREATE TABLE Admin ("
    	+ "userId INTEGER PRIMARY KEY, "
    	+ "FOREIGN KEY (userId) REFERENCES User (userId) ON UPDATE CASCADE ON DELETE CASCADE)";

    static String healthySQLCreate = "CREATE TABLE Healthy ("
        + "userId INTEGER PRIMARY KEY, "
        + "FOREIGN KEY (userId) REFERENCES User (userId) ON UPDATE CASCADE ON DELETE CASCADE)";

    static String infectedSQLCreate = "CREATE TABLE Infected ("
        + "userId INTEGER PRIMARY KEY, "
        + "infectedSince INT ,"
        + "lastCloseContactsCheck REAL ,"
        + "FOREIGN KEY (userId) REFERENCES User (userId) ON UPDATE CASCADE ON DELETE CASCADE)";

    static String suspectSQLCreate = "CREATE TABLE Suspect ("
       + "userId INTEGER PRIMARY KEY, "
       + "suspectSince INT, "
       + "FOREIGN KEY (userId) REFERENCES User (userId) ON UPDATE CASCADE ON DELETE CASCADE)";

    static String locationSQLCreate = "CREATE TABLE Location ("
        + "locationId INTEGER PRIMARY KEY, "
    	+ "userId INTEGER, "
    	+ "time INT, "
    	+ "latitude REAL NOT NULL, "
    	+ "longitude REAL NOT NULL, "
    	+ "FOREIGN KEY (userId) REFERENCES User (userId) ON DELETE CASCADE ON UPDATE CASCADE)";

    static String stateHistorySQLCreate = "CREATE TABLE StateHistory ("
        + "userId INTEGER, "
        + "time REAL NOT NULL, "
        + "oldState TEXT, "
        + "newState TEXT, "
        + "FOREIGN KEY (userId) REFERENCES User (userId) ON UPDATE CASCADE ON DELETE CASCADE)";

    /**
    * El usuario admin (con contraseña admin) será el root por defecto del sistema. 
    * Desde este ususario root se podrán crear mas usuarios admin. Por defecto está healthy.
    */
    static String insertAdminInUser = "INSERT INTO User (userId, username, password) " 
        + "VALUES (1, 'admin', 'admin')";
    static String insertAdminInAdmin = "INSERT INTO Admin " 
        + "VALUES (1)";
    static String insertAdminInHealthy = "INSERT INTO Healthy "
        + "VALUES (1)";

    
    public static void main(String [] args) throws SQLException {
        Connection con = DriverManager.getConnection("jdbc:sqlite:Contactus.db"); 
        Statement stmt = con.createStatement();

        //stmt.execute("PRAGMA foreign_keys = ON"); //enable foreign key behavior

        //tablas
        stmt.executeUpdate(userSQLCreate);
        stmt.executeUpdate(adminSQLCreate);
        stmt.executeUpdate(healthySQLCreate);
        stmt.executeUpdate(infectedSQLCreate);
        stmt.executeUpdate(suspectSQLCreate);
        stmt.executeUpdate(locationSQLCreate);
        stmt.executeUpdate(stateHistorySQLCreate);

        //Insert del usuario root/admin del sistema
        stmt.executeUpdate(insertAdminInUser);
        stmt.executeUpdate(insertAdminInAdmin);
        stmt.executeUpdate(insertAdminInHealthy);
        
    }
    
}
