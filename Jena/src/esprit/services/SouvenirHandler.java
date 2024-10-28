package esprit.services;


import esprit.entities.Souvenir;
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

public class SouvenirHandler implements HttpHandler {
    private static Model model;
    private static String NS;
    private final Gson gson = new Gson();

    public SouvenirHandler(Model model, String namespace) {
        SouvenirHandler.model = model;
        SouvenirHandler.NS = namespace;
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
                response = createSouvenir(exchange);
                break;
            case "GET":
                if (exchange.getRequestURI().getPath().equals("/souvenir")) {
                    response = getAllSouvenirs();
                } else {
                    response = readSouvenir(exchange);
                }
                break;
            case "PUT":
                response = updateSouvenir(exchange);
                break;
            case "DELETE":
                response = deleteSouvenir(exchange);
                break;
            default:
                response = "{\"error\": \"Unsupported method!\"}";
        }
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private String createSouvenir(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
        Souvenir souvenir = gson.fromJson(isr, Souvenir.class);
        Resource souvenirResource = model.createResource(NS + souvenir.getNom());
        souvenirResource.addProperty(RDFS.label, souvenir.getNom());
        souvenirResource.addProperty(model.createProperty(NS + "description"), souvenir.getDescription());
        souvenirResource.addProperty(model.createProperty(NS + "stock"), String.valueOf(souvenir.getStock()));
        souvenirResource.addProperty(model.createProperty(NS + "prix"), String.valueOf(souvenir.getPrix()));
        return "{\"message\": \"Souvenir created: " + souvenir.getNom() + "\"}";
    }

    private String readSouvenir(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String nom = path.split("/")[2];
        List<Souvenir> souvenirs = new ArrayList<>();
        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX ns: <" + NS + "> " +
                        "SELECT ?s ?description ?stock ?prix WHERE { " +
                        "?s rdfs:label \"" + nom + "\" ." +
                        "?s ns:description ?description ." +
                        "?s ns:stock ?stock ." +
                        "?s ns:prix ?prix }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Souvenir souvenir = new Souvenir();
                souvenir.setNom(soln.getResource("s").getLocalName());
                souvenir.setDescription(soln.getLiteral("description").getString());
                souvenir.setStock(soln.getLiteral("stock").getInt());
                souvenir.setPrix(soln.getLiteral("prix").getDouble());
                souvenirs.add(souvenir);
            }
        }
        return gson.toJson(souvenirs);
    }

    private String updateSouvenir(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String oldName = path.split("/")[2];
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
        Souvenir updatedSouvenir = gson.fromJson(isr, Souvenir.class);

        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "DELETE { ?s rdfs:label \"" + oldName + "\" } " +
                "INSERT { " +
                "?s rdfs:label \"" + updatedSouvenir.getNom() + "\" ; " +
                "ns:description \"" + updatedSouvenir.getDescription() + "\" ; " +
                "ns:stock \"" + updatedSouvenir.getStock() + "\" ; " +
                "ns:prix \"" + updatedSouvenir.getPrix() + "\" } " +
                "WHERE { ?s rdfs:label \"" + oldName + "\" }";

        executeUpdate(queryString);
        return "{\"message\": \"Souvenir updated from " + oldName + " to " + updatedSouvenir.getNom() + "\"}";
    }

    private String deleteSouvenir(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String nom = path.split("/")[2];
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "DELETE { ?s rdfs:label \"" + nom + "\" } " +
                "WHERE { ?s rdfs:label \"" + nom + "\" }";
        executeUpdate(queryString);
        return "{\"message\": \"Souvenir deleted: " + nom + "\"}";
    }

    private String getAllSouvenirs() {
        List<Souvenir> souvenirs = new ArrayList<>();
        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX ns: <" + NS + "> " +
                        "SELECT ?s ?nom ?description ?stock ?prix WHERE { " +
                        "?s rdfs:label ?nom ." +
                        "?s ns:description ?description ." +
                        "?s ns:stock ?stock ." +
                        "?s ns:prix ?prix }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Souvenir souvenir = new Souvenir();
                souvenir.setNom(soln.getLiteral("nom").getString());
                souvenir.setDescription(soln.getLiteral("description").getString());
                souvenir.setStock(soln.getLiteral("stock").getInt());
                souvenir.setPrix(soln.getLiteral("prix").getDouble());
                souvenirs.add(souvenir);
            }
        }
        return gson.toJson(souvenirs);
    }

    private void executeUpdate(String updateString) {
        Dataset dataset = DatasetFactory.create(model);
        UpdateRequest updateRequest = UpdateFactory.create(updateString);
        UpdateExecutionFactory.create(updateRequest, dataset).execute();
    }
}
