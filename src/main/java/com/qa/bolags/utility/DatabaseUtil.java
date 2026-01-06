package com.qa.bolags.utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DatabaseUtil {

    private static final String DB_URL = "jdbc:oracle:thin:@10.229.241.160:1521:orcl3";
    private static final String DB_USER = "APP_INQ_MATCH";
    private static final String DB_PASSWORD = "Inqesv1030chdev$2025";

    public static String getOtpFromDatabase(String query) {
        String otp = null;
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {
                otp = resultSet.getString("OTP");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return otp;
    }


    public static String getMFAOtpFromDatabase(String query) {
        String otp = null;
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {
                otp = resultSet.getString("MFA_OTP");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return otp;
    }

}