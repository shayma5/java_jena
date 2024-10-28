package esprit.services;

import esprit.entities.Formateur;
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

public class FormateurHandler implements HttpHandler {
    private static Model model;
    private static String NS;
    private final Gson gson = new Gson();

    public FormateurHandler(Model model, String namespace) {
        FormateurHandler.model = model;
        FormateurHandler.NS = namespace;
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
                response = createFormateur(exchange);
                break;
            case "GET":
                if (exchange.getRequestURI().getPath().equals("/formateurs")) {
                    response = getAllFormateurs();
                } else {
                    response = readFormateur(exchange);
                }
                break;
            case "PUT":
                response = updateFormateur(exchange);
                break;
            case "DELETE":
                response = deleteFormateur(exchange);
                break;
            default:
                response = "{\"error\": \"Unsupported method!\"}";
        }
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private String createFormateur(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
        Formateur formateur = gson.fromJson(isr, Formateur.class);
        Resource formateurResource = model.createResource(NS + formateur.getNom());
        formateurResource.addProperty(RDFS.label, formateur.getNom());
        formateurResource.addProperty(model.createProperty(NS + "biographie"), formateur.getBiographieFormateur());
        formateurResource.addProperty(model.createProperty(NS + "nbrAnneesExperience"), String.valueOf(formateur.getNbrAnneesExperience()));
        formateurResource.addProperty(model.createProperty(NS + "specialite"), formateur.getSpecialiteFormateur());
        formateurResource.addProperty(model.createProperty(NS + "role"), formateur.getRole());
        return "{\"message\": \"Formateur created: " + formateur.getNom() + "\"}";
    }

    private String readFormateur(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String nom = path.split("/")[2];
        List<Formateur> formateurs = new ArrayList<>();
        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX ns: <" + NS + "> " +
                        "SELECT ?s ?biographie ?nbrAnneesExperience ?specialite ?role WHERE { " +
                        "?s rdfs:label \"" + nom + "\" ." +
                        "?s ns:biographie ?biographie ." +
                        "?s ns:nbrAnneesExperience ?nbrAnneesExperience ." +
                        "?s ns:specialite ?specialite ." +
                        "?s ns:role ?role }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Formateur formateur = new Formateur();
                formateur.setNom(soln.getResource("s").getLocalName());
                formateur.setBiographieFormateur(soln.getLiteral("biographie").getString());
                formateur.setNbrAnneesExperience(soln.getLiteral("nbrAnneesExperience").getInt());
                formateur.setSpecialiteFormateur(soln.getLiteral("specialite").getString());
                formateur.setRole(soln.getLiteral("role").getString());
                formateurs.add(formateur);
            }
        }
        return gson.toJson(formateurs);
    }

    private String updateFormateur(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String oldName = path.split("/")[2];
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
        Formateur updatedFormateur = gson.fromJson(isr, Formateur.class);

        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "DELETE { ?s rdfs:label \"" + oldName + "\" } " +
                "INSERT { " +
                "?s rdfs:label \"" + updatedFormateur.getNom() + "\" ; " +
                "ns:biographie \"" + updatedFormateur.getBiographieFormateur() + "\" ; " +
                "ns:nbrAnneesExperience \"" + updatedFormateur.getNbrAnneesExperience() + "\" ; " +
                "ns:specialite \"" + updatedFormateur.getSpecialiteFormateur() + "\" ; " +
                "ns:role \"" + updatedFormateur.getRole() + "\" } " +
                "WHERE { ?s rdfs:label \"" + oldName + "\" }";
        executeUpdate(queryString);
        return "{\"message\": \"Formateur updated from " + oldName + " to " + updatedFormateur.getNom() + "\"}";
    }

    private String deleteFormateur(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String nom = path.split("/")[2];
        String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "DELETE { ?s rdfs:label \"" + nom + "\" } " +
                "WHERE { ?s rdfs:label \"" + nom + "\" }";
        executeUpdate(queryString);
        return "{\"message\": \"Formateur deleted: " + nom + "\"}";
    }

    private String getAllFormateurs() {
        List<Formateur> formateurs = new ArrayList<>();
        String queryString =
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                        "PREFIX ns: <" + NS + "> " +
                        "SELECT ?s ?nom ?biographie ?nbrAnneesExperience ?specialite ?role WHERE { " +
                        "?s rdfs:label ?nom ." +
                        "?s ns:biographie ?biographie ." +
                        "?s ns:nbrAnneesExperience ?nbrAnneesExperience ." +
                        "?s ns:specialite ?specialite ." +
                        "?s ns:role ?role }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Formateur formateur = new Formateur();
                formateur.setNom(soln.getLiteral("nom").getString());
                formateur.setBiographieFormateur(soln.getLiteral("biographie").getString());
                formateur.setNbrAnneesExperience(soln.getLiteral("nbrAnneesExperience").getInt());
                formateur.setSpecialiteFormateur(soln.getLiteral("specialite").getString());
                formateur.setRole(soln.getLiteral("role").getString());
                formateurs.add(formateur);
            }
        }
        return gson.toJson(formateurs);
    }

    private void executeUpdate(String updateString) {
        Dataset dataset = DatasetFactory.create(model);
        UpdateRequest updateRequest = UpdateFactory.create(updateString);
        UpdateExecutionFactory.create(updateRequest, dataset).execute();
    }
}
