package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionService {
    Connection connection;

    public Connection getConnection() throws SQLException {
         return  DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/linelladb"
                , "root"
                , ""
        );
    }
}
