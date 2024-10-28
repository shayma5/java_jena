package esprit.entities;

public class LieuTouristique {
    private String nom; // Nom du lieu touristique
    private String localisation; // Localisation du lieu touristique
    private int capaciteDAccueil; // Capacité d'accueil du lieu

    // Getters et Setters
    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public int getCapaciteDAccueil() {
        return capaciteDAccueil;
    }

    public void setCapaciteDAccueil(int capaciteDAccueil) {
        this.capaciteDAccueil = capaciteDAccueil;
    }
}
