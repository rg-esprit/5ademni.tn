package com.khademni.service;

import com.khademni.model.Client;
import com.khademni.model.Freelancer;
import com.khademni.model.ContratModel;
import com.khademni.exception.ContractCreationException;
import java.util.List;

public interface ContratService {
    List<Client> getAllClients() throws ContractCreationException;

    List<Freelancer> getAllFreelancers() throws ContractCreationException;

    void createContract(ContratModel contrat) throws ContractCreationException;

    void updateContract(ContratModel contrat) throws ContractCreationException;
}
