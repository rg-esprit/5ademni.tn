package com.khademni.service;

import com.khademni.model.OffreModel;
import com.khademni.exception.BusinessException;
import java.util.List;

public interface OffreService {
    List<OffreModel> getAllOffers() throws BusinessException;

    List<OffreModel> getAllDemands() throws BusinessException;

    List<OffreModel> getGlobalFeed() throws BusinessException;

    List<OffreModel> searchPaginated(String query, int offset, int limit) throws BusinessException;

    int countSearchResults(String query) throws BusinessException;

    List<OffreModel> getUserOffres(int userId) throws BusinessException;

    List<OffreModel> getUserDemandes(int userId) throws BusinessException;

    void createOffre(OffreModel offre) throws BusinessException;

    void updateOffre(OffreModel offre) throws BusinessException;

    void deleteOffre(int id, String type) throws BusinessException;
}
