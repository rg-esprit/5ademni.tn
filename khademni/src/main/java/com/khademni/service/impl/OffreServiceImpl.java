package com.khademni.service.impl;

import com.khademni.dao.OffreDAO;
import com.khademni.dao.impl.OffreDAOImpl;
import com.khademni.exception.BusinessException;
import com.khademni.model.OffreModel;
import com.khademni.service.OffreService;
import com.khademni.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class OffreServiceImpl implements OffreService {
    private static final Logger logger = LoggerFactory.getLogger(OffreServiceImpl.class);
    private final OffreDAO offreDAO = new OffreDAOImpl();

    @Override
    public List<OffreModel> getAllOffers() throws BusinessException {
        try {
            return offreDAO.findByType("OFFRE");
        } catch (SQLException e) {
            logger.error("Error fetching offers", e);
            throw new BusinessException("Erreur lors de la récupération des offres.");
        }
    }

    @Override
    public List<OffreModel> getAllDemands() throws BusinessException {
        try {
            return offreDAO.findByType("DEMANDE");
        } catch (SQLException e) {
            logger.error("Error fetching demands", e);
            throw new BusinessException("Erreur lors de la récupération des demandes.");
        }
    }

    @Override
    public void createOffre(OffreModel offre) throws BusinessException {
        validateOffre(offre);
        try {
            offreDAO.save(offre);
        } catch (SQLException e) {
            logger.error("Error creating offre", e);
            throw new BusinessException("Erreur lors de la création.");
        }
    }

    @Override
    public void updateOffre(OffreModel offre) throws BusinessException {
        validateOffre(offre);
        try {
            offreDAO.update(offre);
        } catch (SQLException e) {
            logger.error("Error updating offre", e);
            throw new BusinessException("Erreur lors de la mise à jour.");
        }
    }

    @Override
    public void deleteOffre(int id, String type) throws BusinessException {
        try {
            ((OffreDAOImpl) offreDAO).deleteWithType(id, type);
        } catch (SQLException e) {
            logger.error("Error deleting offre", e);
            throw new BusinessException("Erreur lors de la suppression.");
        }
    }

    private void validateOffre(OffreModel offre) throws BusinessException {
        if (offre.getTitre() == null || offre.getTitre().trim().isEmpty()) {
            throw new BusinessException("Le titre est obligatoire.");
        }
        if (offre.getPrix() < 0) {
            throw new BusinessException("Le prix ne peut pas être négatif.");
        }
        // prix == 0 : ne pas afficher le prix (affiché comme "----")
        offre.setTitre(ValidationUtils.sanitize(offre.getTitre()));
        offre.setDescription(ValidationUtils.sanitize(offre.getDescription()));
    }
}
