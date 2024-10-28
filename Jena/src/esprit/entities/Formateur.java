package esprit.entities;

public class Formateur {
    private String nom;
    private String biographieFormateur;
    private int nbrAnneesExperience;
    private String specialiteFormateur;
    private String role;

    // Getters et Setters
    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getBiographieFormateur() {
        return biographieFormateur;
    }

    public void setBiographieFormateur(String biographieFormateur) {
        this.biographieFormateur = biographieFormateur;
    }

    public int getNbrAnneesExperience() {
        return nbrAnneesExperience;
    }

    public void setNbrAnneesExperience(int nbrAnneesExperience) {
        this.nbrAnneesExperience = nbrAnneesExperience;
    }

    public String getSpecialiteFormateur() {
        return specialiteFormateur;
    }

    public void setSpecialiteFormateur(String specialiteFormateur) {
        this.specialiteFormateur = specialiteFormateur;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}