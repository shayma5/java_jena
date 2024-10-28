package esprit.services;

import esprit.entities.LieuCulte;
import esprit.tools.JenaEngine;
import org.apache.jena.rdf.model.*;
import org.apache.jena.query.*;
import org.apache.jena.update.*;
import org.apache.jena.vocabulary.RDFS;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LieuCulteHandler implements HttpHandler {
    private static Model model;
    private static String NS;
    private final Gson gson = new Gson();

    public LieuCulteHandler(Model model, String namespace) {
        LieuCulteHandler.model = model;
        LieuCulteHandler.NS = namespace;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost:4200");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // Handle preflight OPTIONS request
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1); // No content for OPTIONS request
            return;
        }
        String response;
        switch (exchange.getRequestMethod()) {
            case "POST":
                response = createLieuCulte(exchange);
                break;
            case "GET":
                if (exchange.getRequestURI().getPath().equals("/lieuCulte")) {
                    response = getAllLieuCultes();
                } else {
                    response = readLieuCulte(exchange);
                }
                break;
            case "PUT":
                response = updateLieuCulte(exchange);
                break;
            case "DELETE":
                response = deleteLieuCulte(exchange);
                break;
            default:
                response = "{\"error\": \"Unsupported method!\"}";
        }
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private String createLieuCulte(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
        LieuCulte lieuCulte = gson.fromJson(isr, LieuCulte.class);
        Resource lieuCulteResource = model.createResource(NS + lieuCulte.getNom());
        lieuCulteResource.addProperty(RDFS.label, lieuCulte.getNom());
        lieuCulteResource.addProperty(model.createProperty(NS + "description"), lieuCulte.getDescription());
        lieuCulteResource.addProperty(model.createProperty(NS + "capaciteDAccueil"), String.valueOf(lieuCulte.getCapaciteDAccueil()));
        lieuCulteResource.addProperty(model.createProperty(NS + "localisation"), lieuCulte.getLocalisation());
        return "{\"message\": \"LieuCulte created: " + lieuCulte.getNom() + "\"}";
    }

    private String readLieuCulte(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String nom = path.split("/")[2];
        List<LieuCulte> lieuxCultes = new ArrayList<>();
        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX ns: <" + NS + "> " +
                        "SELECT ?s ?description ?capaciteDAccueil ?localisation WHERE { " +
                        "?s rdfs:label \"" + nom + "\" ." +
                        "?s ns:description ?description ." +
                        "?s ns:capaciteDAccueil ?capaciteDAccueil ." +
                        "?s ns:localisation ?localisation }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                LieuCulte lieuCulte = new LieuCulte();
                lieuCulte.setNom(soln.getResource("s").getLocalName());
                lieuCulte.setDescription(soln.getLiteral("description").getString());
                lieuCulte.setCapaciteDAccueil(soln.getLiteral("capaciteDAccueil").getInt());
                lieuCulte.setLocalisation(soln.getLiteral("localisation").getString());
                lieuxCultes.add(lieuCulte);
            }
        }
        return gson.toJson(lieuxCultes);
    }

    private String updateLieuCulte(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String oldName = path.split("/")[2];
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
        LieuCulte updatedLieuCulte = gson.fromJson(isr, LieuCulte.class);

        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "DELETE { ?s rdfs:label \"" + oldName + "\" } " +
                "INSERT { ?s rdfs:label \"" + updatedLieuCulte.getNom() + "\" } " +
                "WHERE { ?s rdfs:label \"" + oldName + "\" }";
        executeUpdate(queryString);

        // Update properties
        Resource lieuCulteResource = model.getResource(NS + oldName);
        lieuCulteResource.removeProperties(); // Clear existing properties
        lieuCulteResource.addProperty(RDFS.label, updatedLieuCulte.getNom());
        lieuCulteResource.addProperty(model.createProperty(NS + "description"), updatedLieuCulte.getDescription());
        lieuCulteResource.addProperty(model.createProperty(NS + "capaciteDAccueil"), String.valueOf(updatedLieuCulte.getCapaciteDAccueil()));
        lieuCulteResource.addProperty(model.createProperty(NS + "localisation"), updatedLieuCulte.getLocalisation());
        return "{\"message\": \"LieuCulte updated from " + oldName + " to " + updatedLieuCulte.getNom() + "\"}";
    }

    private String deleteLieuCulte(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String nom = path.split("/")[2];
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "DELETE { ?s rdfs:label \"" + nom + "\" } " +
                "WHERE { ?s rdfs:label \"" + nom + "\" }";
        executeUpdate(queryString);
        return "{\"message\": \"LieuCulte deleted: " + nom + "\"}";
    }

    private String getAllLieuCultes() {
        List<LieuCulte> lieuxCultes = new ArrayList<>();
        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX ns: <" + NS + "> " +
                        "SELECT ?s ?nom ?description ?capaciteDAccueil ?localisation WHERE { " +
                        "?s rdfs:label ?nom ." +
                        "?s ns:description ?description ." +
                        "?s ns:capaciteDAccueil ?capaciteDAccueil ." +
                        "?s ns:localisation ?localisation }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                LieuCulte lieuCulte = new LieuCulte();
                lieuCulte.setNom(soln.getLiteral("nom").getString());
                lieuCulte.setDescription(soln.getLiteral("description").getString());
                lieuCulte.setCapaciteDAccueil(soln.getLiteral("capaciteDAccueil").getInt());
                lieuCulte.setLocalisation(soln.getLiteral("localisation").getString());
                lieuxCultes.add(lieuCulte);
            }
        }
        return gson.toJson(lieuxCultes);
    }

    private void executeUpdate(String updateString) {
        Dataset dataset = DatasetFactory.create(model);
        UpdateRequest updateRequest = UpdateFactory.create(updateString);
        UpdateExecutionFactory.create(updateRequest, dataset).execute();
    }
}
