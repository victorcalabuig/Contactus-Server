package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Clase para crear/conectarse a la base de datos del servidor y crear las tablas
 * necesarias (de momento User y Location).
 */
public class DatabaseCreation {
    
    static String userSQLCreate = "CREATE TABLE IF NOT EXISTS User("
            + "userId INT PRIMARY KEY,"
            + "email String UNIQUE,"
            + "password String NOT NULL)";
    
    
    public static void main(String [] args) throws SQLException {
        Connection con = DriverManager.getConnection("jdbc:sqlite:Contactus.db"); 
        Statement stmt = con.createStatement();
        stmt.executeUpdate(userSQLCreate); //Creación de la tabla User en la BBDD
    }
    
}
