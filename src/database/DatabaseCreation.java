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
        + "password TEXT NOT NULL)";
    
    static String adminSQLCreate = "CREATE TABLE Admin ("
    	+ "userId INTEGER PRIMARY KEY, "
    	+ "FOREIGN KEY (userId) REFERENCES User (userId))";

    static String locationSQLCreate = "CREATE TABLE Location ("
    	+ "userId INTEGER, "
    	+ "time INT, "
    	+ "latitude REAL NOT NULL, "
    	+ "longitude REAL NOT NULL, "
    	+ "PRIMARY KEY (userId, time), "
    	+ "FOREIGN KEY (userId) REFERENCES User (userId) ON DELETE CASCADE )";	

    
    public static void main(String [] args) throws SQLException {
        Connection con = DriverManager.getConnection("jdbc:sqlite:Contactus.db"); 
        Statement stmt = con.createStatement();
        stmt.executeUpdate(userSQLCreate); 
        stmt.executeUpdate(adminSQLCreate); 
        stmt.executeUpdate(locationSQLCreate); 
    }
    
}
