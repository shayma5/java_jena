package esprit.services;

import esprit.entities.ActiviteAgricole;
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

public class ActiviteAgricoleHandler implements HttpHandler {
    private static Model model;
    private static String NS;
    private final Gson gson = new Gson();

    public ActiviteAgricoleHandler(Model model, String namespace) {
        ActiviteAgricoleHandler.model = model;
        ActiviteAgricoleHandler.NS = namespace;
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
                response = createActiviteAgricole(exchange);
                break;
            case "GET":
                if (exchange.getRequestURI().getPath().equals("/activiteAgricole")) {
                    response = getAllActivitesAgricoles();
                } else {
                    response = readActiviteAgricole(exchange);
                }
                break;
            case "PUT":
                response = updateActiviteAgricole(exchange);
                break;
            case "DELETE":
                response = deleteActiviteAgricole(exchange);
                break;
            default:
                response = "{\"error\": \"Unsupported method!\"}";
        }

        // Send the response
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }


    private String createActiviteAgricole(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
        ActiviteAgricole activite = gson.fromJson(isr, ActiviteAgricole.class);
        Resource activiteAgricole = model.createResource(NS + activite.getNom());
        activiteAgricole.addProperty(RDFS.label, activite.getNom());
        activiteAgricole.addProperty(model.createProperty(NS + "duree"), activite.getDuree());
        activiteAgricole.addProperty(model.createProperty(NS + "prix"), String.valueOf(activite.getPrix()));
        activiteAgricole.addProperty(model.createProperty(NS + "localisation"), activite.getLocalisation());
        activiteAgricole.addProperty(model.createProperty(NS + "nbrParticipants"), String.valueOf(activite.getNbrParticipants()));
        activiteAgricole.addProperty(model.createProperty(NS + "description"), activite.getDescription());
        return "{\"message\": \"ActiviteAgricole created: " + activite.getNom() + "\"}";
    }

    private String readActiviteAgricole(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String nom = path.split("/")[2];
        List<ActiviteAgricole> activites = new ArrayList<>();
        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX ns: <" + NS + "> " +
                        "SELECT ?s ?duree ?prix ?localisation ?nbrParticipants ?description WHERE { " +
                        "?s rdfs:label \"" + nom + "\" ." +
                        "?s ns:duree ?duree ." +
                        "?s ns:prix ?prix ." +
                        "?s ns:localisation ?localisation ." +
                        "?s ns:nbrParticipants ?nbrParticipants ." +
                        "?s ns:description ?description }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                ActiviteAgricole activite = new ActiviteAgricole();
                activite.setNom(soln.getResource("s").getLocalName());
                activite.setDuree(soln.getLiteral("duree").getString());
                activite.setPrix(soln.getLiteral("prix").getDouble());
                activite.setLocalisation(soln.getLiteral("localisation").getString());
                activite.setNbrParticipants(soln.getLiteral("nbrParticipants").getInt());
                activite.setDescription(soln.getLiteral("description").getString());
                activites.add(activite);
            }
        }
        return gson.toJson(activites);
    }

    private String updateActiviteAgricole(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String oldName = path.split("/")[2];
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
        String newName = new BufferedReader(isr).readLine();

        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "DELETE { ?s rdfs:label \"" + oldName + "\" } " +
                "INSERT { ?s rdfs:label \"" + newName + "\" } " +
                "WHERE { ?s rdfs:label \"" + oldName + "\" }";
        executeUpdate(queryString);
        return "{\"message\": \"ActiviteAgricole updated from " + oldName + " to " + newName + "\"}";
    }

    private String deleteActiviteAgricole(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String nom = path.split("/")[2];
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "DELETE { ?s rdfs:label \"" + nom + "\" } " +
                "WHERE { ?s rdfs:label \"" + nom + "\" }";
        executeUpdate(queryString);
        return "{\"message\": \"ActiviteAgricole deleted: " + nom + "\"}";
    }

    private String getAllActivitesAgricoles() {
        List<ActiviteAgricole> activites = new ArrayList<>();
        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX ns: <" + NS + "> " +
                        "SELECT ?s ?nom ?duree ?prix ?localisation ?nbrParticipants ?description WHERE { " +
                        "?s rdfs:label ?nom ." +
                        "?s ns:duree ?duree ." +
                        "?s ns:prix ?prix ." +
                        "?s ns:localisation ?localisation ." +
                        "?s ns:nbrParticipants ?nbrParticipants ." +
                        "?s ns:description ?description }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                ActiviteAgricole activite = new ActiviteAgricole();
                activite.setNom(soln.getLiteral("nom").getString());
                activite.setDuree(soln.getLiteral("duree").getString());
                activite.setPrix(soln.getLiteral("prix").getDouble());
                activite.setLocalisation(soln.getLiteral("localisation").getString());
                activite.setNbrParticipants(soln.getLiteral("nbrParticipants").getInt());
                activite.setDescription(soln.getLiteral("description").getString());
                activites.add(activite);
            }
        }
        return gson.toJson(activites);
    }

    private void executeUpdate(String updateString) {
        Dataset dataset = DatasetFactory.create(model);
        UpdateRequest updateRequest = UpdateFactory.create(updateString);
        UpdateExecutionFactory.create(updateRequest, dataset).execute();
    }
}