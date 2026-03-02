package com.khademni.dao;

import com.khademni.model.Freelancer;
import java.sql.SQLException;
import java.util.List;

public interface FreelancerDAO {
    List<Freelancer> findAll() throws SQLException;
}
