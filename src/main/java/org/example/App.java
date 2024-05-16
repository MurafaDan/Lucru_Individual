package org.example;

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DatabaseViewer viewer = new DatabaseViewer();
                viewer.setVisible(true);
            }
        });
    }
}

