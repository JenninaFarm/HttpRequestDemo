package fi.tamk.hello;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
     * If verbose is true, this class will print out messages to the Gdx log.
     */
    private static boolean verbose = false;

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
                if (verbose)
                    Gdx.app.log("HighScoreServer", "Fetch: success");
                source.receiveHighScore(highScores);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
//        Net.HttpRequest request = new Net.HttpRequest(HttpMethods.GET);
//        request.setUrl(url);
//        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
//            @SuppressWarnings("unchecked")
//            @Override
//            public void handleHttpResponse (Net.HttpResponse httpResponse) {
//                String r = httpResponse.getResultAsString();
//                //System.out.println(r);
//                JsonValue jsonObject = (new JsonReader().parse(r));
//
//                ArrayList<HighScoreEntry> highScores = new ArrayList<>();
//
//                for (int i = 1; i <= 10; i++) {
//                    HighScoreEntry score = new HighScoreEntry(
//                            jsonObject.get(i).get(0).asString(),
//                            jsonObject.get(i).get(1).asInt());
//                    highScores.add(score);
//                }
//                if (verbose)
//                    Gdx.app.log("HighScoreServer", "Fetch: success");
//                source.receiveHighScore(highScores);
//            }
//
//            @Override
//            public void failed (Throwable t) {
//                if (verbose)
//                    Gdx.app.error("HighScoreServer",
//                            "Fetch: failed");
//                source.failedToRetrieveHighScores(t);
//            }
//
//            @Override
//            public void cancelled () {
//                if (verbose)
//                    Gdx.app.log("HighScoreServer", "Fetch: cancelled");
//            }
//
//        });
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
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);

        String content = "name=" + highScore.getName() +
                "&score=" + highScore.getScore() +
                "&user=" + user +
                "&password=" + password;

        Net.HttpRequest request = new Net.HttpRequest(HttpMethods.POST);
        request.setUrl(url);
        request.setContent(content);

        if (user == null) {
            if (verbose)
                Gdx.app.error("HighSCoreServer", "user not set");
        } else if (password == null) {
            if (verbose)
                Gdx.app.error("HighSCoreServer", "password not set");
        } else if (url == null) {
            if (verbose)
                Gdx.app.error("HighSCoreServer", "url not set");
        } else {
            Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    if (verbose)
                        Gdx.app.log("HighScoreServer", "Send: success");
                    source.receiveSendReply(httpResponse);
                }

                @Override
                public void failed(Throwable t) {
                    if (verbose)
                        Gdx.app.error("HighScoreServer",
                                "Send: failed", t);
                    source.failedToSendHighScore(t);
                }

                @Override
                public void cancelled() {
                    if (verbose)
                        Gdx.app.log("HighScoreServer", "Send: cancelled");
                }

            });
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

    public static String getGetUrl() {
        return url;
    }

    public static void setUrl(String getUrl) {
        HighScoreServer.url = getUrl;
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public static void setVerbose(boolean verbose) {
        HighScoreServer.verbose = verbose;
    }
}