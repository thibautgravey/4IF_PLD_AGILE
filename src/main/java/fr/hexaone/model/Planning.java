package fr.hexaone.model;

import fr.hexaone.algo.AlgoTSP;
import javafx.beans.property.SimpleStringProperty;
import org.javatuples.Pair;

import java.util.*;

/**
 * Objet contenant toutes les informations relatives au planning d'une tournée "
 *
 * @author HexaOne
 * @version 1.0
 */
public class Planning {

    /**
     * L'id du Dépôt associé au planning
     */
    protected Long idDepot;

    /**
     * Date de début de la tournée. 24 hours format - H:m:s
     */
    protected Date dateDebut;

    /**
     * Date de la fin de la tournée / du retour au dépot
     */
    protected Date dateFin;

    /**
     * Liste des requêtes en rapport avec la demande client
     */
    protected List<Requete> requetes;

    /**
     * Liste des ids uniques d'intersections constituant la tournée
     */
    protected List<Demande> demandesOrdonnees;

    /**
     * Carte associée au planning
     */
    protected Carte carte;

    /**
     * liste de tous les trajets composant la tournée
     */
    protected List<Trajet> listeTrajets;

    /**
     * Duree totale de la tournée en secondes
     */
    protected Integer dureeTotale;

    /**
     * Map permettant d'identifier les chemins les plus courts à partir d'un
     * identifiant (String)
     */
    protected Map<String, Trajet> TrajetsLesPlusCourts;

    /**
     * Comparateur afin de classer les chromosomes au sein d'une population dans
     * l'ordre croissant des coûts
     */
    public Comparator<Pair<List<Demande>, Double>> ComparatorChromosome = (e1,
            e2) -> (int) (e1.getValue1() - e2.getValue1());

    ///////////////////////////////
    // Méthode de plannification //
    ///////////////////////////////

    /**
     * Constructeur du planning
     * 
     * @param carte La carte du planning
     */
    public Planning(Carte carte) {
        this.requetes = new ArrayList<>();
        this.carte = carte;
    }

    /**
     * Constructeur du planning
     * 
     * @param carte La carte du planning
     */
    public Planning(Carte carte, List<Requete> requetes) {
        this.requetes = requetes;
        this.carte = carte;
    }

    /**
     * Recherche de la tournée la plus rapide : - Crée une la liste ordonnée de
     * demandes demandesOrdonnées - Crée la liste des trajets et calcul les durées
     * de passage et de sorties dans les demandes
     * 
     * A appeler qu'une fois pour générer le premier ordonnancement
     * 
     * Prérequis : - La liste des requetes - La date de début de la tournée -
     * idDépot
     * 
     * @return Vrai si succès
     */
    public boolean calculerMeilleurTournee() {

        // Recherche des chemins des plus courts entre toutes les
        // intersections spéciales (dépots, livraisons et dépot)
        List<Intersection> intersectionsSpeciales = new ArrayList<>();
        intersectionsSpeciales.add(carte.getIntersections().get(idDepot));
        for (Requete r : requetes) {
            intersectionsSpeciales.add(carte.getIntersections().get(r.getDemandeCollecte().getIdIntersection()));
            intersectionsSpeciales.add(carte.getIntersections().get(r.getDemandeLivraison().getIdIntersection()));

        }
        boolean success = calculerLesTrajetsLesPlusCourts(intersectionsSpeciales);
        if (!success) {
            return false;
        }

        // recherche de la melleure tournéee
        List<Object> demandes = new ArrayList<>();
        for (Requete requete : requetes) {
            demandes.add(requete.getDemandeCollecte());
            demandes.add(requete.getDemandeLivraison());
        }

        AlgoTSP algo = new AlgoTSP(this.idDepot, this.TrajetsLesPlusCourts);

        List<Object> result = algo.algoGenetique(demandes);

        this.demandesOrdonnees = new ArrayList<>();

        for (Object obj : result) {
            this.demandesOrdonnees.add((Demande) obj);
        }

        // Création de la listes des trajets à suivre et calcul des temps
        ordonnerLesTrajetsEtLesDates();

        return true;
    }

    /**
     * Recrée une liste de trajets à partir des demandes ordonnées et calcul des
     * dates de passage dans les demandes.
     * 
     * A utiliser après un changement d'ordonnancement des demandes.
     * 
     * Prérequis : - Avoir les demandes ordonnées
     */
    public void ordonnerLesTrajetsEtLesDates() {
        long prevIntersectionId = idDepot;
        listeTrajets = new LinkedList<>();
        double duree = 0.;
        long tempsDebut = dateDebut.getTime();

        for (Demande demande : demandesOrdonnees) {
            Long newId = demande.getIdIntersection();
            Trajet trajet = TrajetsLesPlusCourts.get(prevIntersectionId + "|" + newId);
            listeTrajets.add(trajet);
            prevIntersectionId = newId;

            duree += trajet.getPoids() * 3600. / 15.;
            demande.setDateArrivee(new Date(tempsDebut + (long) duree));
            duree += demande.getDuree() * 1000;
            demande.setDateDepart(new Date(tempsDebut + (long) duree));
        }

        Trajet trajet = TrajetsLesPlusCourts.get(prevIntersectionId + "|" + idDepot);
        listeTrajets.add(trajet);
        duree += trajet.getPoids() * 3600. / 15.;
        dateFin = new Date(tempsDebut + (long) duree);
        dureeTotale = (int) duree / 1000;
    }

    /**
     * Recalcule tous les plus courts trajets entre toutes demandes. - Crée un
     * nouvelle liste de trajets ordonnées et calcul les durées de passage et de
     * sorties dans les demandes.
     * 
     * A utiliser après l'ajout ou la supression de demandes ou la modification des
     * temps et lieux de demandes.
     * 
     * Prérequis : - Avoir les demandes ordonnées.
     * 
     * @return Vrai si succès
     */
    public boolean recalculerTournee() {
        // Recalcule tous les plus courts trajets des demandes
        List<Intersection> intersectionsSpeciales = new ArrayList<>();
        intersectionsSpeciales.add(carte.getIntersections().get(idDepot));
        for (Demande demande : demandesOrdonnees) {
            intersectionsSpeciales.add(carte.getIntersections().get(demande.getIdIntersection()));
        }
        boolean success = calculerLesTrajetsLesPlusCourts(intersectionsSpeciales);
        if (!success) {
            return false;
        }

        // Recréer une liste de trajet et recalcule les dates des demandes
        ordonnerLesTrajetsEtLesDates();
        return true;
    }

    /**
     * Ajouter une demande seule après avoir déja calculer la meilleure tournée.
     * 
     * @return Vrai si succès
     */
    public boolean ajouterDemande(Demande demande) {
        demandesOrdonnees.add(demande);

        boolean success = recalculerTournee();
        
        if ( !success ) demandesOrdonnees.remove(demande);
        
        return success;
    }

    /**
     * Modifier une demande seule après avoir déja calculer la meilleure tournée.
     * 
     * @return Vrai si succès
     */
    public boolean modifierDemande(Demande demande, int duree, Long idIntersection) {

        int prevDuree = demande.getDuree();
        Long prevIdInter = demande.getIdIntersection();
        
        String nom = null;
        for (Segment s : carte.getIntersections().get(idIntersection)
                .getSegmentsArrivants()) {
            if (!s.getNom().isEmpty()) {
                nom = s.getNom();
                break;
            }
        }

        
        demande.setDuree(duree);
        demande.setIdIntersection(idIntersection);
        demande.setNomIntersectionProperty(new SimpleStringProperty(nom));
        
        boolean success = recalculerTournee();
        if ( !success ) {
            demande.setDuree(prevDuree);
            demande.setIdIntersection(prevIdInter);
        }

        return success;
    }

    /**
     * Ajouter une requete après avoir déja calculé la meilleure tournée.
     * 
     * @return Vrai si succès
     */
    public boolean ajouterRequete(Requete requete) {
        requetes.add(requete);

        demandesOrdonnees.add(requete.getDemandeCollecte());
        demandesOrdonnees.add(requete.getDemandeLivraison());

        boolean success = recalculerTournee();

        if ( !success ) {
            demandesOrdonnees.remove(requete.getDemandeCollecte());
            demandesOrdonnees.remove(requete.getDemandeLivraison());
        }

        return success;
    }

    /**
     * Ajouter une requete après avoir déja calculer la meilleure tournée.
     * 
     * @return Vrai si succès
     */
    public boolean ajouterRequete(Requete requete, List<Integer> positions) {
        requetes.add(requete);

        demandesOrdonnees.add(positions.get(0), requete.getDemandeCollecte());
        demandesOrdonnees.add(positions.get(1), requete.getDemandeLivraison());

        return recalculerTournee();
    }

    /**
     * Supprimer une requete de la tournée et regénère les trajets ordonées
     */
    public void supprimerDemande(Demande demande) {
        demandesOrdonnees.remove(demande);

        ordonnerLesTrajetsEtLesDates();

    }

    /**
     * Supprimer une requete de la tournée et regénère les trajets ordonées
     * 
     * @return la position de la collecte et de la livraison
     */
    public List<Integer> supprimerRequete(Requete requete) {
        requetes.remove(requete);

        List<Integer> positions = new ArrayList<>();

        positions.add(demandesOrdonnees.indexOf(requete.getDemandeCollecte()));
        positions.add(demandesOrdonnees.indexOf(requete.getDemandeLivraison()));

        demandesOrdonnees.remove(requete.getDemandeCollecte());
        demandesOrdonnees.remove(requete.getDemandeLivraison());

        ordonnerLesTrajetsEtLesDates();

        return positions;
    }

    /**
     * Modifer la durée d'une demande
     */
    public void modifierDemande(Demande demande, Integer duree) {
        demande.setDuree(duree);

        ordonnerLesTrajetsEtLesDates();
    }

    /**
     * Modifer la durée d'une demande
     * 
     * @return Vrai si succès
     */
    public boolean modifierDemande(Demande demande, Long idIntersection) {
        Long prevIdIntersection = demande.getIdIntersection();

        demande.setIdIntersection(idIntersection);

        boolean success = recalculerTournee();
        if ( !success ) {
            demande.setIdIntersection(prevIdIntersection);
        }

        return success;
    }

    /**
     * Modifer la durée et l'intersection d'une demande
     * 
     * @return Vrai si succès
     */
    public boolean modifierDemande(Demande demande, Long idIntersection, Integer duree) {
        Long prevIdIntersection = demande.getIdIntersection();
        
        demande.setIdIntersection(idIntersection);
        demande.setDuree(duree);

        boolean success = recalculerTournee();

        if ( !success ) {
            demande.setIdIntersection(prevIdIntersection);
        }

        return success;
    }
    
    public void reinitialiserPlanning() {
    	
    	this.demandesOrdonnees=null;
    	this.dateDebut=null;
    	this.dateFin=null;
    	this.dureeTotale=null;
    	this.idDepot=null;
    	this.listeTrajets=null;
    	this.requetes.clear();

    }

    ///////////////////////////////////////////////
    // Algo de recherche des plus courts trajets //
    ///////////////////////////////////////////////

    /**
     * Calculer tous les trajets les plus courts entre toutes les intersections en
     * paramètre
     * 
     * @param intersections La liste des intersections
     * 
     * @return Vrai si succès
     */
    public boolean calculerLesTrajetsLesPlusCourts(List<Intersection> intersections) {

        // Préparation

        Map<Long, Intersection> allIntersections = carte.getIntersections();

        TrajetsLesPlusCourts = new HashMap<>();

        // Calcul de tous les chemins les plus courts n fois avec dijkstra

        for (Intersection source : intersections) {

            source.setDistance(0.);
            Set<Intersection> settledIntersections = new HashSet<>();
            Set<Intersection> unsettledIntersections = new HashSet<>();

            unsettledIntersections.add(source);

            while (unsettledIntersections.size() != 0) {

                Intersection currentIntersection = getLowestDistanceIntersection(unsettledIntersections);
                unsettledIntersections.remove(currentIntersection);

                for (Segment segmentAdjacent : currentIntersection.getSegmentsPartants()) {
                    Intersection adjacentIntersection = allIntersections.get(segmentAdjacent.getArrivee());
                    Double edgeWeight = segmentAdjacent.getLongueur();
                    if (!settledIntersections.contains(adjacentIntersection)) {
                        CalculateMinimumDistance(adjacentIntersection, edgeWeight, currentIntersection,
                                segmentAdjacent);
                        unsettledIntersections.add(adjacentIntersection);
                    }
                }
                settledIntersections.add(currentIntersection);
            }

            String sourceId = source.getId() + "|";

            for (Intersection i : intersections) {
                String key = sourceId + i.getId();
                if(i.getDistance() >= Double.MAX_VALUE) {
                    return false;
                }
                TrajetsLesPlusCourts.put(key, new Trajet(i.getCheminLePlusCourt(), i.getDistance()));
            }

            allIntersections.forEach((id, intersection) -> intersection.resetIntersection());
        }

        return true;
    }

    /**
     * Retourne l'intersection avec la distance la plus faible
     * 
     * @param unsettledIntersections Les intersections non parcourus
     * @return lowestDistanceIntersection : l'intersection la plus proche
     */
    public Intersection getLowestDistanceIntersection(Set<Intersection> unsettledIntersections) {
        Intersection lowestDistanceIntersection = null;
        double lowestDistance = Double.MAX_VALUE;
        for (Intersection intersection : unsettledIntersections) {
            double intersectionDistance = intersection.getDistance();
            if (intersectionDistance < lowestDistance) {
                lowestDistance = intersectionDistance;
                lowestDistanceIntersection = intersection;
            }
        }
        return lowestDistanceIntersection;
    }

    /**
     * Enregistre la distance minimale pour accéder à une intersection
     * 
     * @param evaluationIntersection L'intersection à évaluer
     * @param edgeWeigh Le poids de l'arrête
     * @param sourceIntersection L'intersection source
     * @param seg Le segment associé
     */
    public void CalculateMinimumDistance(Intersection evaluationIntersection, Double edgeWeigh,
            Intersection sourceIntersection, Segment seg) {
        Double sourceDistance = sourceIntersection.getDistance();
        if (sourceDistance + edgeWeigh < evaluationIntersection.getDistance()) {
            evaluationIntersection.setDistance(sourceDistance + edgeWeigh);
            LinkedList<Segment> shortestPath = new LinkedList<>(sourceIntersection.getCheminLePlusCourt());
            shortestPath.add(seg);
            evaluationIntersection.setCheminLePlusCourt(shortestPath);
        }
    }

    ///////////////////////
    // GETTER AND SETTER //
    ///////////////////////

    /**
     * Getter
     * @return L'id du dépot
     */
    public Long getIdDepot() {
        return idDepot;
    }

    /**
     * Setter
     * @param idDepot L'id du dépot
     */
    public void setIdDepot(Long idDepot) {
        this.idDepot = idDepot;
    }

    /**
     * Getter
     * @return La date de début
     */
    public Date getDateDebut() {
        return dateDebut;
    }

    /**
     * Setter
     * @param dateDebut La date de début
     */
    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    /**
     * Getter
     * @return La liste des requêtes
     */
    public List<Requete> getRequetes() {
        return requetes;
    }

    /**
     * Setter
     * @param requetes La liste des requêtes
     */
    public void setRequetes(List<Requete> requetes) {
        this.requetes = requetes;
    }

    /**
     * Getter
     * @return La liste des demandes ordonnées
     */
    public List<Demande> getDemandesOrdonnees() {
        return demandesOrdonnees;
    }

    /**
     * Getter
     * @return La carte
     */
    public Carte getCarte() {
        return carte;
    }

    /**
     * Setter
     * @param carte La carte
     */
    public void setCarte(Carte carte) {
        this.carte = carte;
    }

    /**
     * Getter
     * @return La liste des trajets
     */
    public List<Trajet> getListeTrajets() {
        return listeTrajets;
    }

    /**
     * Getter
     * @return La durée totale
     */
    public Integer getDureeTotale() {
        return dureeTotale;
    }

    /**
     * Getter
     * @return La liste des plus courts trajets
     */
    public Map<String, Trajet> getTrajetsLesPlusCourts() {
        return TrajetsLesPlusCourts;
    }

    /**
     * Getter
     * @return La date de fin
     */
    public Date getDateFin() {
        return dateFin;
    }
}
