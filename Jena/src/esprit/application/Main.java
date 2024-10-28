package esprit.application;

import esprit.tools.JenaEngine;
import org.apache.jena.rdf.model.Model;
import com.sun.net.httpserver.HttpServer;

import java.io.FileOutputStream;
import java.net.InetSocketAddress;

public class Main {

    private static Model model;
    private static String NS;

    public static void main(String[] args) throws Exception {
        model = JenaEngine.readModel("data/projet.rdf");
        if (model != null) {
            NS = model.getNsPrefixURI("");

            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/activiteAgricole", new esprit.services.ActiviteAgricoleHandler(model, NS));
            server.createContext("/souvenir", new esprit.services.SouvenirHandler(model, NS));
            server.createContext("/formateurs", new esprit.services.FormateurHandler(model, NS));
            server.createContext("/lieuCulte", new esprit.services.LieuCulteHandler(model, NS));
            server.createContext("/lieuTouristique", new esprit.services.LieuTouristiqueHandler(model, NS));
            server.setExecutor(null);
            server.start();
            System.out.println("Server is running on port 8080");

            // Add a shutdown hook to save the model before the application stops
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                saveModel(model, "data/projet.rdf");
            }));
        } else {
            System.out.println("Model is null.");
        }
    }

    private static void saveModel(Model model, String filePath) {
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            model.write(out, "RDF/XML");
            System.out.println("Model saved to " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
