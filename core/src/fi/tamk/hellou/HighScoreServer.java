package fi.tamk.hellou;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * To get this work on an android device add following line to
 * AndroidManifest.xml:
 * <uses-permission android:name="android.permission.INTERNET" />
 * above tag <application
 */
public class HighScoreServer {
    /**
     * url is the address of the highscore server.
     */
    private static String url;

    /**
     * Password for the highscore host.
     */
    private static String password;

    /**
     * Username for the highscore host.
     */
    private static String user;

    /**
     * fetchHighScores gets high score entries from the server.
     * <p>
     * This gets high score entries from a server, converts the json file to
     * List of HighScoreEntries and sends it back to the source.
     *
     * @param source source class implementing HighScoreListener
     */
    public static void fetchHighScores(final HighScoreListener source) {
        // Load CAs from an InputStream
        // (could be from a resource or ByteArrayInputStream or ...)
        HttpsURLConnection urlConnection = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            Properties prop = new Properties();
            FileHandle file = Gdx.files.internal("tamk-pythonohj-2021-it-tuni-fi.pem");
            InputStream inputStream = file.read();

            InputStream caInput = new BufferedInputStream(inputStream);
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            URL connectionURL = new URL(url);

            urlConnection = (HttpsURLConnection) connectionURL.openConnection();
            urlConnection.setSSLSocketFactory(context.getSocketFactory());

            if (urlConnection.getResponseCode() == 200) {
                InputStream in = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null) {
                    sb.append(output);
                }

                JsonValue jsonObject = (new JsonReader().parse(sb.toString()));

                ArrayList<HighScoreEntry> highScores = new ArrayList<>();

                for (int i = 1; i <= 10; i++) {
                    JsonValue entry = jsonObject.get(String.valueOf(i));
                    if (entry != null) {
                        HighScoreEntry score = new HighScoreEntry(
                                entry.get(0).asString(),
                                entry.get(1).asInt());
                        highScores.add(score);
                    }
                    else {
                        break;
                    }
                }
                source.receiveHighScore(highScores);
            } else {
                // something went wrong
                source.failedToRetrieveHighScores("Connection couldn't be established.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * sendHighScore entry sends new high score data to the server
     * <p>
     * It will then send a confirmation of success back to the source.
     *
     * @param highScore The new HighScoreEntry data to be sent to the server.
     * @param source    source class implementing HighScoreListener
     */
    public static void sendNewHighScore(HighScoreEntry highScore,
                                        final HighScoreListener source) {
        HttpsURLConnection urlConnection = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Properties prop = new Properties();
            FileHandle file = Gdx.files.internal("tamk-pythonohj-2021-it-tuni-fi.pem");
            InputStream inputStream = file.read();
            InputStream caInput = new BufferedInputStream(inputStream);
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }
            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);
            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);
            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            URL connectionURL = new URL(url);

            urlConnection = (HttpsURLConnection) connectionURL.openConnection();
            urlConnection.setSSLSocketFactory(context.getSocketFactory());

            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            OutputStream os = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(String.format("name=%s&score=%d&user=%s&password=%s", highScore.getName(), highScore.getScore(), user, password));
            writer.flush();
            writer.close();
            os.close();
            urlConnection.connect();
            int response = urlConnection.getResponseCode();
            if ( response == 200)
            {
                InputStream in = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String output;
                while ((output = br.readLine()) != null) {
                    sb.append(output);
                }
                JsonValue jsonObject = (new JsonReader().parse(sb.toString()));

                source.receiveSendReply(response);
            }
            else {
                // Something went wrong
                source.failedToSendHighScore("Connection couldn't be established.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static void readConfig(String propFileName) {
        Properties prop = new Properties();
        FileHandle file = Gdx.files.internal(propFileName);
        InputStream inputStream = file.read();

        try {
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Config file '" + propFileName + "' not found.");
            }
        } catch (IOException e) {
            Gdx.app.log("HighScoreServer", e.getMessage());
        }

        user = prop.getProperty("user");
        password = prop.getProperty("password");
        url = prop.getProperty("url");
    }
}