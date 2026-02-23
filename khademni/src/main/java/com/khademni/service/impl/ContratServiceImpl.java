package com.khademni.service.impl;

import com.khademni.dao.ClientDAO;
import com.khademni.dao.FreelancerDAO;
import com.khademni.dao.ContratDAO;
import com.khademni.dao.impl.ClientDAOImpl;
import com.khademni.dao.impl.FreelancerDAOImpl;
import com.khademni.dao.impl.ContratDAOImpl;
import com.khademni.model.Client;
import com.khademni.model.Freelancer;
import com.khademni.model.ContratModel;
import com.khademni.service.ContratService;
import com.khademni.exception.ContractCreationException;
import com.khademni.utils.MyDataBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ContratServiceImpl implements ContratService {
    private static final Logger logger = LoggerFactory.getLogger(ContratServiceImpl.class);

    private final ClientDAO clientDAO = new ClientDAOImpl();
    private final FreelancerDAO freelancerDAO = new FreelancerDAOImpl();
    private final ContratDAO contratDAO = new ContratDAOImpl();

    @Override
    public List<Client> getAllClients() throws ContractCreationException {
        try {
            return clientDAO.findAll();
        } catch (SQLException e) {
            logger.error("Error fetching clients", e);
            throw new ContractCreationException("Failed to load clients list.");
        }
    }

    @Override
    public List<Freelancer> getAllFreelancers() throws ContractCreationException {
        try {
            return freelancerDAO.findAll();
        } catch (SQLException e) {
            logger.error("Error fetching freelancers", e);
            throw new ContractCreationException("Failed to load freelancers list.");
        }
    }

    @Override
    public void createContract(ContratModel contrat) throws ContractCreationException {
        validateContract(contrat);
        try (Connection conn = MyDataBase.getConnection()) {
            conn.setAutoCommit(false);
            try {
                contratDAO.save(contrat, conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Error saving contract, rolling back", e);
                throw new ContractCreationException("Database error: Could not save contract.");
            }
        } catch (SQLException e) {
            logger.error("Connection error during transaction", e);
            throw new ContractCreationException("System error: Database connection failed.");
        }
    }

    @Override
    public void updateContract(ContratModel contrat) throws ContractCreationException {
        validateContract(contrat);
        try (Connection conn = MyDataBase.getConnection()) {
            conn.setAutoCommit(false);
            try {
                contratDAO.update(contrat, conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Error updating contract, rolling back", e);
                throw new ContractCreationException("Database error: Could not update contract.");
            }
        } catch (SQLException e) {
            logger.error("Connection error during transaction", e);
            throw new ContractCreationException("System error: Database connection failed.");
        }
    }

    private void validateContract(ContratModel contrat) throws ContractCreationException {
        if (contrat.getIdClient() == 0 || contrat.getIdFreelancer() == 0) {
            throw new ContractCreationException("Client and Freelancer must be selected.");
        }
        if (contrat.getPrix() <= 0) {
            throw new ContractCreationException("Contract amount must be greater than zero.");
        }
    }
}
