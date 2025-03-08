package com.etljobs.sql2json2api.service.orchestration;

import java.util.Map;

import lombok.Getter;

/**
 * Classe représentant une erreur survenue lors du traitement d'une ligne de résultat SQL.
 * Elle conserve le contexte de l'erreur pour faciliter le diagnostic et le traitement.
 */
@Getter
public class RowError {
    private final int rowIndex;
    private final Map<String, Object> rowData;
    private final String errorMessage;
    private final Exception exception;
    private final int attempts;
    
    /**
     * Constructeur avec tous les détails de l'erreur.
     * 
     * @param rowIndex Index de la ligne qui a échoué (0-based)
     * @param rowData Données de la ligne qui a échoué
     * @param errorMessage Message d'erreur
     * @param exception Exception qui a causé l'erreur
     * @param attempts Nombre de tentatives effectuées avant échec définitif
     */
    public RowError(int rowIndex, Map<String, Object> rowData, String errorMessage, Exception exception, int attempts) {
        this.rowIndex = rowIndex;
        this.rowData = rowData;
        this.errorMessage = errorMessage;
        this.exception = exception;
        this.attempts = attempts;
    }
    
    /**
     * Obtient l'index lisible pour l'utilisateur (1-based au lieu de 0-based)
     * 
     * @return Index de la ligne pour affichage
     */
    public int getDisplayRowIndex() {
        return rowIndex + 1;
    }
    
    /**
     * Vérifie si l'erreur contient une exception.
     * 
     * @return true si une exception est présente, false sinon
     */
    public boolean hasException() {
        return exception != null;
    }
    
    /**
     * Obtient le type d'exception s'il est disponible.
     * 
     * @return Nom de la classe d'exception ou "Unknown" si non disponible
     */
    public String getExceptionType() {
        return exception != null ? exception.getClass().getSimpleName() : "Unknown";
    }
    
    /**
     * Crée un résumé de l'erreur avec le contexte.
     * 
     * @return Message formaté avec contexte
     */
    public String getFormattedMessage() {
        return String.format("Erreur à la ligne %d (après %d tentatives): %s", 
                getDisplayRowIndex(), attempts, errorMessage);
    }
    
    /**
     * Format d'affichage standard de l'erreur.
     */
    @Override
    public String toString() {
        return getFormattedMessage();
    }
}