import helpers.RecommendationTuner;
import helpers.Utils;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.Play;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class Global extends GlobalSettings {

    RecommendationTuner tuner = new RecommendationTuner();

    int N_TRIES_TO_FETCH_ONTOLOGIES = 10;
    int DELAY_BETWEEN_FETCHES = 1000;

    private class fetchOntologiesFileThread extends Thread
    {
        private int nAttempts;


        public void fetchJSONIntoFile(String myURL, String targetFilePath) throws IOException
        {
			String msg = "Fetching ontologies from URL:" + myURL;
			
            System.out.println(msg);
            Logger.info(msg);
			
            URLConnection urlConn = null;
            InputStreamReader in = null;

            URL url = new URL(myURL);
            urlConn = url.openConnection();
            urlConn.addRequestProperty("Accept", "application/json");

            if (urlConn != null)
                urlConn.setReadTimeout(60 * 1000);

            if (urlConn != null && urlConn.getInputStream() != null) {
                in = new InputStreamReader(urlConn.getInputStream(),
                        Charset.defaultCharset());

                BufferedReader bufferedReader = new BufferedReader(in);

                File targetFile = new File(targetFilePath);

                if(targetFile.exists())
                {
					msg = "File " + targetFilePath + " exists. Deleting...";
                    System.out.println(msg);
		            Logger.info(msg);
					
					
                    targetFile.delete();
					
					msg = "File " + targetFilePath + " deleted.";
					System.out.println(msg);
					Logger.info(msg);
                }

				msg = "Creating new file at " + targetFilePath;
                System.out.println(msg);
				Logger.info(msg);				

                try
                {
                    File file = new File(targetFilePath);
                    File parentDirectory = new File(file.getParentFile().getAbsolutePath());

                    if(!parentDirectory.exists())
                    {
                        parentDirectory.mkdirs();
                        System.out.println("Created directory at : " + parentDirectory.getAbsolutePath());
                    }

                    targetFile.createNewFile();
                    msg = "Created new file at " + targetFilePath;
                    System.out.println(msg);
                    Logger.info(msg);

                    FileWriter targetFileWriter = new FileWriter(targetFile);

                    if (bufferedReader != null) {
                        int cp;
                        while ((cp = bufferedReader.read()) != -1) {
                            targetFileWriter.append((char) cp);
                        }
                        bufferedReader.close();
                    }

                    targetFileWriter.close();

                    msg = "Finished downloading ontologies file. File is at: " + targetFilePath;
                    System.out.println(msg);
                    Logger.info(msg);
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }

            in.close();
        }


        private void fetchOntologiesFile(int nAttempts)
        {
            String allOntologiesURL = Play.application().configuration().getString("ontologies.all_ontologies_uri");
            String allOntologiesCacheFile = Utils.getAbsFilePath(Play.application().configuration().getString("ontologies.all_ontologies_cache_file"));

            if(nAttempts < 0)
            {
                System.out.println("Failed to retrieve active Ontologies file from active Dendro instance at "+allOntologiesURL+". Killing recommender");
                System.exit(80); //for being detected as an error by the systemd system
            }
            else
            {
                String msg = "Fetching active metadata ontologies from Dendro Instance... (TRY Nº " + (N_TRIES_TO_FETCH_ONTOLOGIES - nAttempts + 1) + " of " + N_TRIES_TO_FETCH_ONTOLOGIES + ")";
                Logger.info(msg);
                System.out.println(msg);

                try
                {
                    fetchJSONIntoFile(allOntologiesURL, allOntologiesCacheFile);
                }
                catch (IOException e)
                {
                    try
                    {
                        Thread.sleep(DELAY_BETWEEN_FETCHES);
                    } catch (InterruptedException e1)
                    {
                        e1.printStackTrace();
                    }

                    System.err.println("Error fetching ontologies: " + e.getLocalizedMessage());
                    e.printStackTrace();
                    fetchOntologiesFile(nAttempts - 1);
                }
            }
        }


        @Override
        public void run()
        {
            fetchOntologiesFile(N_TRIES_TO_FETCH_ONTOLOGIES);
        }
    }

    @Override
    public void onStart(Application app) {
        String msg = "Starting up recommendation monitor / tuner thread...";

        Logger.info(msg);
        System.out.println(msg);

        tuner.setPriority(Thread.MIN_PRIORITY);
        tuner.setDaemon(true);
        tuner.start();
        msg = "Recommendation monitor / tuner thread RUNNING!";

        Logger.info(msg);
        System.out.println(msg);

        msg = "Fetching active metadata ontologies from Dendro Instance...";
        Logger.info(msg);
        System.out.println(msg);
        new fetchOntologiesFileThread().run();

        msg = "Ontologies fetched!";
        Logger.info(msg);
        System.out.println(msg);
    }

    @Override
    public void onStop(Application app) {
        String msg = "Joining recommendation monitor / tuner thread...";
        try {
            tuner.shutdown();
            tuner.join(2000);
        } catch (InterruptedException e) {
            System.out.println("Unable to wait for tuner thread joining");
            e.printStackTrace();
        }
        Logger.info(msg);
        System.out.println(msg);
    }

}
