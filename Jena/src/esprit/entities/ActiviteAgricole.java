package esprit.entities;

public class ActiviteAgricole {
    private String nom;
    private String duree;
    private double prix;
    private String localisation;
    private int nbrParticipants;
    private String description;

    // Getters et Setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDuree() { return duree; }
    public void setDuree(String duree) { this.duree = duree; }
    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }
    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }
    public int getNbrParticipants() { return nbrParticipants; }
    public void setNbrParticipants(int nbrParticipants) { this.nbrParticipants = nbrParticipants; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}