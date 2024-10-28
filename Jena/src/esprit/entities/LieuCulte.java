package esprit.entities;

public class LieuCulte {
    private String nom;
    private String description;
    private int capaciteDAccueil; // Capacité d'accueil du lieu
    private String localisation; // Localisation du lieu

    // Getters et Setters
    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCapaciteDAccueil() {
        return capaciteDAccueil;
    }

    public void setCapaciteDAccueil(int capaciteDAccueil) {
        this.capaciteDAccueil = capaciteDAccueil;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }
}