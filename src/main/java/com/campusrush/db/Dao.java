package com.campusrush.db;

import com.campusrush.core.exceptions.DataAccessException;
import java.util.List;

/**
 * Generic DAO (Data Access Object) interface demonstrating Unit 2 & 4 Generics requirement.
 * @param <T> Entity type
 * @param <ID> Primary key identifier type
 */
public interface Dao<T, ID> {
    T findById(ID id) throws DataAccessException;
    List<T> findAll() throws DataAccessException;
    ID insert(T entity) throws DataAccessException;
    void update(T entity) throws DataAccessException;
    void delete(ID id) throws DataAccessException;
}
