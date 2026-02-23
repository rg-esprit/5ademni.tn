package com.khademni.service;

import com.khademni.model.OffreModel;
import com.khademni.exception.BusinessException;
import java.util.List;

public interface OffreService {
    List<OffreModel> getAllOffers() throws BusinessException;

    List<OffreModel> getAllDemands() throws BusinessException;

    void createOffre(OffreModel offre) throws BusinessException;

    void updateOffre(OffreModel offre) throws BusinessException;

    void deleteOffre(int id, String type) throws BusinessException;
}
