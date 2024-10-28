package esprit.services;

import esprit.entities.LieuTouristique;
import esprit.tools.JenaEngine; // Adjust if you don't need this import
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

public class LieuTouristiqueHandler implements HttpHandler {
    private static Model model;
    private static String NS;
    private final Gson gson = new Gson();

    public LieuTouristiqueHandler(Model model, String namespace) {
        LieuTouristiqueHandler.model = model;
        LieuTouristiqueHandler.NS = namespace;
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
                response = createLieuTouristique(exchange);
                break;
            case "GET":
                if (exchange.getRequestURI().getPath().equals("/lieuTouristique")) {
                    response = getAllLieuxTouristiques();
                } else {
                    response = readLieuTouristique(exchange);
                }
                break;
            case "PUT":
                response = updateLieuTouristique(exchange);
                break;
            case "DELETE":
                response = deleteLieuTouristique(exchange);
                break;
            default:
                response = "{\"error\": \"Unsupported method!\"}";
        }
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private String createLieuTouristique(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
        LieuTouristique lieu = gson.fromJson(isr, LieuTouristique.class);
        Resource lieuTouristique = model.createResource(NS + lieu.getNom());
        lieuTouristique.addProperty(RDFS.label, lieu.getNom());
        lieuTouristique.addProperty(model.createProperty(NS + "localisation"), lieu.getLocalisation());
        lieuTouristique.addProperty(model.createProperty(NS + "capaciteDAccueil"), String.valueOf(lieu.getCapaciteDAccueil()));
        return "{\"message\": \"LieuTouristique created: " + lieu.getNom() + "\"}";
    }

    private String readLieuTouristique(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String nom = path.split("/")[2];
        List<LieuTouristique> lieux = new ArrayList<>();
        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX ns: <" + NS + "> " +
                        "SELECT ?s ?localisation ?capaciteDAccueil WHERE { " +
                        "?s rdfs:label \"" + nom + "\" ." +
                        "?s ns:localisation ?localisation ." +
                        "?s ns:capaciteDAccueil ?capaciteDAccueil }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                LieuTouristique lieu = new LieuTouristique();
                lieu.setNom(soln.getResource("s").getLocalName());
                lieu.setLocalisation(soln.getLiteral("localisation").getString());
                lieu.setCapaciteDAccueil(soln.getLiteral("capaciteDAccueil").getInt());
                lieux.add(lieu);
            }
        }
        return gson.toJson(lieux);
    }

    private String updateLieuTouristique(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String oldName = path.split("/")[2];
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
        LieuTouristique updatedLieu = gson.fromJson(isr, LieuTouristique.class);

        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "DELETE { ?s rdfs:label \"" + oldName + "\" } " +
                "INSERT { " +
                "?s rdfs:label \"" + updatedLieu.getNom() + "\" ; " +
                "ns:localisation \"" + updatedLieu.getLocalisation() + "\" ; " +
                "ns:capaciteDAccueil \"" + updatedLieu.getCapaciteDAccueil() + "\" . } " +
                "WHERE { ?s rdfs:label \"" + oldName + "\" }";
        executeUpdate(queryString);
        return "{\"message\": \"LieuTouristique updated from " + oldName + " to " + updatedLieu.getNom() + "\"}";
    }

    private String deleteLieuTouristique(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String nom = path.split("/")[2];
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "DELETE { ?s rdfs:label \"" + nom + "\" } " +
                "WHERE { ?s rdfs:label \"" + nom + "\" }";
        executeUpdate(queryString);
        return "{\"message\": \"LieuTouristique deleted: " + nom + "\"}";
    }

    private String getAllLieuxTouristiques() {
        List<LieuTouristique> lieux = new ArrayList<>();
        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX ns: <" + NS + "> " +
                        "SELECT ?s ?nom ?localisation ?capaciteDAccueil WHERE { " +
                        "?s rdfs:label ?nom ." +
                        "?s ns:localisation ?localisation ." +
                        "?s ns:capaciteDAccueil ?capaciteDAccueil }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                LieuTouristique lieu = new LieuTouristique();
                lieu.setNom(soln.getLiteral("nom").getString());
                lieu.setLocalisation(soln.getLiteral("localisation").getString());
                lieu.setCapaciteDAccueil(soln.getLiteral("capaciteDAccueil").getInt());
                lieux.add(lieu);
            }
        }
        return gson.toJson(lieux);
    }

    private void executeUpdate(String updateString) {
        Dataset dataset = DatasetFactory.create(model);
        UpdateRequest updateRequest = UpdateFactory.create(updateString);
        UpdateExecutionFactory.create(updateRequest, dataset).execute();
    }
}
