package fr.hexaone.controller.State;

import fr.hexaone.controller.Controleur;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Implémentation d'un State représentant l'état initial de l'application
 * 
 * @author HexaOne
 * @version 1.0
 */
public class EtatInitial implements State {

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Controleur c) {
        c.getFenetre().initFenetreInitial();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void chargerRequetes(Controleur c) {
        Alert messageAlerte = new Alert(AlertType.INFORMATION);
        messageAlerte.setTitle("Information");
        messageAlerte.setHeaderText(null);
        messageAlerte.setContentText("Vous devez charger une carte avant de pouvoir charger des requêtes !");
        messageAlerte.showAndWait();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void lancerCalcul(Controleur c) {
        Alert messageAlerte = new Alert(AlertType.INFORMATION);
        messageAlerte.setTitle("Information");
        messageAlerte.setHeaderText(null);
        messageAlerte
                .setContentText("Vous devez charger une carte et des requêtes avant de pouvoir lancer le calcul !");
        messageAlerte.showAndWait();
    }
}
