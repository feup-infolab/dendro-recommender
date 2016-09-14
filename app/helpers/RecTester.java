/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helpers;

import org.codehaus.jettison.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

/**
 *
 * @author joaorocha
 */
public class RecTester {


    private static final String user = "201000919";
    private static final String project = "icpsrviiib2";

     /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SQLException, IOException, JSONException, Exception{
        // PREPARE TABLE FOR RECOMMENDATION

        System.out.println("DONE");
    }


    public static String loadQuery() {

        String s = "";
        StringBuffer sb = new StringBuffer();

        try {
            FileReader fr = new FileReader(new File("fetch_interactions.sql"));
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
