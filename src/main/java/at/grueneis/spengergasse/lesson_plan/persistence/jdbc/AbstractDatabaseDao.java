/*
 * Joachim Grüneis
 * Copyright (C) 2013
 * All rights reserved.
 */

import at.grueneis.spengergasse.lesson_plan.domain.BasePersistable;
import at.grueneis.spengergasse.lesson_plan.persistence.jdbc.DatabaseDao;
import at.grueneis.spengergasse.lesson_plan.persistence.jdbc.LessonPlanDataAccessException;
import at.grueneis.spengergasse.registry.EFPersistable;
import at.grueneis.spengergasse.registry.EntityAlreadyAddedException;
import at.grueneis.spengergasse.registry.EntityNotFoundException;
import at.grueneis.spengergasse.registry.Registry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.lang.reflect.*;

/**
 * "template pattern"
 */
public abstract class AbstractDatabaseDao<T extends BasePersistable> implements DatabaseDao<T>, TableMapper {

    private final Connection connection;
    private String FIND_ALL;
    private String FIND_BY_ID;
    private String INSERT;
    private String UPDATE;
    private String DELETE;
    private Map<String, PreparedStatement> prepMap;
    private Class type;
    

    public AbstractDatabaseDao(Connection connection) {
        this.connection = connection;
        FIND_ALL = "Find all Objects";
        FIND_BY_ID = "Find Object by ID";
        INSERT = "Insert Object";
        UPDATE = "Update Object";
        DELETE ="Delete Object";
        fillMap();
        this.type = ((Class) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    public void fillMap()
    {
    	QueryStatementFactory a = new QueryStatementFactory();
    	prepMap.put(FIND_ALL, a.findAllStatement(this, connection));
    	prepMap.put(FIND_BY_ID, a.FindByIdStatement(this, connection));
    	prepMap.put(INSERT, a.InsertStatement(this, connection));
    	prepMap.put(UPDATE, a.UpdateStatement(this, connection));
       	prepMap.put(DELETE,  a.DeleteStatement(this, connection));
    	
    }
    protected Connection connection() {
        try {
            if (connection == null) {
                throw new IllegalStateException("Connection must not be null");
            }
            if (connection.isClosed()) {
                throw new IllegalStateException("Connection must not be closed");
            }
        } catch (SQLException e) {
            throw new LessonPlanDataAccessException("Failed to validate connection", e);
        }
        return connection;
    }

   
    
    protected abstract String idColumnName();

    protected abstract ArrayList<String> otherColumnNames();

    private String columnList() {
        StringBuffer columnList = new StringBuffer();
        columnList.append(idColumnName());
        for (String columnName : otherColumnNames()) {
            columnList.append(", ").append(columnName);
        }
        return columnList.toString();
    }

    protected abstract String tableName();

    protected abstract T bind(ResultSet resultSet);

    protected abstract void setValuesOfOtherColumnsIntoStatement(PreparedStatement preparedStatement, T entity);

    public final List<T> findAll() {
       QueryStatementExecution<T> a = new QueryStatementExecution<T>(prepMap.get(FIND_ALL));
       return a.execute();
       
    }

    public final T findById(Long id) {
    	try{
    		Registry.getInstance().get(id, type);
    		QueryStatementExecution<T> a = new QueryStatementExecution<T>(prepMap.get(FIND_BY_ID));
       	 	return a.executeSingle(id);
    	}
    	catch(EntityNotFoundException e){
    		System.err.print("Das Objekt mit dieser ID ist nicht in der Registry");
    	}
    	
    	
    	 
    }

    public void save(T t) {
        if (t.getId() == null) {
        try{	
        	//t.setId(idGen);
        	Registry.getInstance().add((EFPersistable)t);
        	DMLStatementExecution<T> a = new DMLStatementExecution<T>(prepMap.get(INSERT)),new Insert());
        	a.execute(t);
        }
        catch(EntityAlreadyAddedException e){
        	System.err.print("Object is already in the Registry");     	
        }
        } else {
        	Registry.getInstance().forceAdd((EFPersistable)t);
        	DMLStatementExecution<T> a = new DMLStatementExecution<T>(prepMap.get(UPDATE)),new Update());
        	a.execute(t);

        }
    }

    
    public void delete(T t) {
    	try{
    		Registry.getInstance().delete((EFPersistable)t);
    		DMLStatementExecution<T> a = new DMLStatementExecution<T>(prepMap.get(DELETE)),new Delete());
    		a.execute(t);
    	}
    	catch(EntityNotFoundException e)
    	{
    		System.err.print("Das Objekt ist nicht in der Registry enthalten");
    	}
    }

    public void delete(Long id) {
    	try{
    		Registry.getInstance().delete(id, type);
    		T t = findById(id);
    		DMLStatementExecution<T> a = new DMLStatementExecution<T>(prepMap.get(DELETE)),new Delete());
    		a.execute(t);
    	}
    	catch(EntityNotFoundException e){
    		System.err.print("Die ID existiert im Registry nicht");
    	}
    		
    	
        
    }

    protected abstract PreparedStatement addValuesToStatement(PreparedStatement P);
    protected abstract PreparedStatement addValuesANDIDToStatement(PreparedStatement P);


}
