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

    /**
    * El usuario admin (con contraseña admin) será el root por defecto del sistema. 
    * Desde este ususario root se podrán crear mas usuarios admin.
    */
    static String insertAdminInUser = "INSERT INTO User (userId, username, password) " 
        + "VALUES (1, 'admin', 'admin')";
    static String insertAdminInAdmin = "INSERT INTO Admin " 
        + "VALUES (1)";

    
    public static void main(String [] args) throws SQLException {
        Connection con = DriverManager.getConnection("jdbc:sqlite:Contactus.db"); 
        Statement stmt = con.createStatement();

        //tablas
        stmt.executeUpdate(userSQLCreate); 
        stmt.executeUpdate(adminSQLCreate); 
        stmt.executeUpdate(locationSQLCreate); 

        //Insert del usuario root del sistema
        stmt.executeUpdate(insertAdminInUser);
        stmt.executeUpdate(insertAdminInAdmin);
        
    }
    
}
