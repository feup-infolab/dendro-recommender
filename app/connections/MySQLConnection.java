package connections;

import com.jcraft.jsch.Session;
import play.Play;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by joaorocha on 15/05/15.
 */
public class MySQLConnection {
    // Logger
    private final static Logger LOGGER =
            Logger.getLogger(MySQLConnection.class.getName());

    /**Connection parameters**/
    private final static String MYSQL_HOST = Play.application().configuration().getString("persistence.mysql.host");
    private final static int MYSQL_PORT = Play.application().configuration().getInt("persistence.mysql.port");
    private final static String MYSQL_USER = Play.application().configuration().getString("persistence.mysql.username");
    private final static String MYSQL_PASSWORD = Play.application().configuration().getString("persistence.mysql.password");
    private final static String MYSQL_DATABASE = Play.application().configuration().getString("persistence.mysql.database");

    public static Connection connection;
    private static Session session;

    public static void close() throws SQLException {
        connection.close();
        session.disconnect();
    }

    public static PreparedStatement prepareStatement (String query) throws SQLException {
        if(connection == null || connection.isClosed())
        {
            MySQLConnection.connect();
        }

        PreparedStatement statement = connection.prepareStatement(query);
        return statement;
    }

    public static Connection connect() throws SQLException{

        if(connection != null && !connection.isClosed())
        {
            return connection;
        }
        else
        {
            StringBuilder url =
                    new StringBuilder("jdbc:mysql://");

            // use assigned_port to establish database connection
            url.append(MYSQL_HOST).
                    append(":").
                    append(MYSQL_PORT).
                    append ("/").
                    append(MYSQL_DATABASE).
                    append ("?user=").
                    append(MYSQL_USER).
                    append ("&password=").
                    append (MYSQL_PASSWORD);

            try {

                System.out.println("Attempting to connect to MySQL at " + MYSQL_HOST + ":" + MYSQL_PORT + ", database name " + MYSQL_DATABASE);

                Class.forName(
                        "com.mysql.jdbc.Driver").newInstance();
                java.sql.Connection connection =
                        java.sql.DriverManager.getConnection(url.toString());

                MySQLConnection.connection = connection;

                System.out.println("Recommender connected to MySQL at " + MYSQL_HOST + ":" + MYSQL_PORT + ", database name " + MYSQL_DATABASE);
                return MySQLConnection.connection;

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                e.printStackTrace();
                System.err.println(e.getMessage());
                System.exit(1);
            }

            MySQLConnection.connection = null;
            return MySQLConnection.connection;
        }
    }

    public static String loadQuery(String filePath) {

        String s = "";
        StringBuffer sb = new StringBuffer();

        try {
            FileReader fr = new FileReader(new File(filePath));
            // be sure to not have line starting with "--" or "/*" or any other non aphabetical character

            BufferedReader br = new BufferedReader(fr);

            while ((s = br.readLine()) != null) {
                sb.append(" " + s);
            }
            br.close();

            // here is our splitter ! We use ";" as a delimiter for each request
            // then we are sure to have well formed statements
            String[] inst = sb.toString().split(";");

            return sb.toString();

        } catch (Exception e) {
            System.out.println("*** Error : " + e.toString());
            System.out.println("*** ");
            System.out.println("*** Error : ");
            e.printStackTrace();
            System.out.println("################################################");
            System.out.println(sb.toString());
        }

        return "NOPE";
    }

}
