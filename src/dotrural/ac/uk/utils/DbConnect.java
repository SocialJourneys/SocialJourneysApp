package dotrural.ac.uk.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import dotrural.ac.uk.constants.PredefinedConstants;

public class DbConnect {
	
	 Connection con = null;
	

	 public Connection getDbConnect() {
		 
	        try {
	            con = DriverManager.getConnection("jdbc:postgresql:"+PredefinedConstants.DATABASE_URL, PredefinedConstants.DATABASE_USERNAME, PredefinedConstants.DATABASE_PASSWORD);
	            //return con;
	        } catch (SQLException ex) {
	            Logger lgr = Logger.getLogger(DbConnect.class.getName());
	            lgr.log(Level.SEVERE, ex.getMessage(), ex);

	        }
			return con; 
	        
	    }
	 
	 public void closeDbConnect() throws SQLException {
		if (con !=null) { 
		 con.close();
		}
	 }
	 
	 
	
	
}
