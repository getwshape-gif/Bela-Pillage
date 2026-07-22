package fr.belarion.belapillage.storage;

import fr.belarion.belapillage.durability.DurabilityManager;

import java.io.IOException;

/**
 * Contrat de persistance pour les degats de durabilite. Une seule implementation pour
 * l'instant ({@link FlatFileDurabilityStorage}), mais isolee derriere une interface pour
 * pouvoir brancher facilement un autre backend (SQLite, etc.) sans toucher au reste du
 * plugin si le besoin se presente un jour.
 */
public interface DurabilityStorage {

    /**
     * Charge les donnees persistees dans le manager. Appele une seule fois, de facon
     * synchrone, pendant onEnable (avant que le serveur n'accepte des explosions).
     */
    void load(DurabilityManager manager) throws IOException;

    /**
     * Sauvegarde l'etat courant du manager. Peut etre appele de facon synchrone
     * (onDisable) ou depuis une tache asynchrone (sauvegarde periodique) : cette methode
     * ne doit donc jamais toucher a l'API Bukkit, uniquement lire la map fournie par
     * DurabilityManager (ConcurrentHashMap, donc iterable en toute securite depuis un
     * thread autre que le thread principal pendant que celui-ci continue d'ecrire dedans).
     */
    void save(DurabilityManager manager) throws IOException;
}
