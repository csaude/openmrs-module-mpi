package org.openmrs.module.fgh.mpi;

import static java.util.Collections.synchronizedList;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.openmrs.api.APIException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class InsertPatients {
	
	private final int SIZE = 50;
	
	private final List<CompletableFuture<Void>> futures = synchronizedList(new ArrayList(SIZE));
	
	private final ExecutorService executor = Executors.newFixedThreadPool(SIZE);
	
	@Test
	public void addPatients() throws Exception {
		final String ID_PH = "{i}";
		final String G_PH = "{g}";
		final String M_PH = "{m}";
		final String H_PH = "{h}";
		final String person = "INSERT INTO person (person_id,gender,birthdate,creator,date_created,voided,uuid) VALUES ("
		        + ID_PH + ", '" + G_PH + "', '2000-01-01', 1, now(), 0, 'person-uuid-" + ID_PH + "');";
		final String name = "INSERT INTO person_name (person_id,given_name,family_name,creator,date_created,voided,uuid) VALUES ("
		        + ID_PH + ", 'John" + ID_PH + "', 'Doe" + ID_PH + "', 1, now(), 0, 'name-uuid-" + ID_PH + "');";
		final String address = "INSERT INTO person_address (person_id,address1,creator,date_created,voided,uuid) VALUES ("
		        + ID_PH + ", '123 Ocean Dr " + ID_PH + "', 1, now(), 0, 'address-uuid-" + ID_PH + "');";
		final String phones = "INSERT INTO person_attribute (person_attribute_type_id,person_id,value,voided,creator,date_created,uuid) VALUES (8, "
		        + ID_PH + ", '" + M_PH + "', 0, 1, now(), 'mobile-uuid-" + ID_PH + "'), (9, " + ID_PH + ", '" + H_PH
		        + "', 0, 1, now(), 'home-uuid-" + ID_PH + "');";
		final String patient = "INSERT INTO patient (patient_id,creator,date_created) VALUES (" + ID_PH + ", 1, now());";
		final String id = "INSERT INTO patient_identifier (patient_id,identifier,identifier_type,location_id,creator,date_created,voided,uuid) VALUES ("
		        + ID_PH + ", '" + ID_PH + "', 2, 1, 1, now(), 0, 'id-uuid-" + ID_PH + "');";
		
		int start = 2;
		int count = 5000000;
		ComboPooledDataSource ds = new ComboPooledDataSource();
		ds.setJdbcUrl("jdbc:mysql://localhost:3310/openmrs");
		ds.setUser("root");
		ds.setPassword("root");
		ds.setInitialPoolSize(SIZE);
		ds.setMaxPoolSize(SIZE);
		
		for (int i = start; i < (count + start); i++) {
			String gender = i % 2 == 0 ? "M" : "F";
			String personQuery = person.replace(ID_PH, "" + i).replace(G_PH, gender);
			String nameQuery = name.replace(ID_PH, "" + i);
			String addressQuery = address.replace(ID_PH, "" + i);
			String patientQuery = patient.replace(ID_PH, "" + i);
			String idQuery = id.replace(ID_PH, "" + i);
			
			futures.add(CompletableFuture.runAsync(() -> {
				Connection c = null;
				Statement s = null;
				try {
					c = ds.getConnection();
					s = c.createStatement();
					s.executeUpdate(personQuery);
					s.executeUpdate(nameQuery);
					s.executeUpdate(addressQuery);
					s.executeUpdate(patientQuery);
					s.executeUpdate(idQuery);
				}
				catch (Throwable e) {
					throw new APIException(e);
				}
				finally {
					try {
						s.close();
						c.close();
					}
					catch (SQLException throwables) {
						throwables.printStackTrace();
					}
				}
				
			}, executor));
			
			if (futures.size() == SIZE || i == (count + start - 1)) {
				CompletableFuture<Void> allFuture = CompletableFuture
				        .allOf(futures.toArray(new CompletableFuture[futures.size()]));
				
				System.out.println("Committing at Index: " + i);
				allFuture.get();
				
				futures.clear();
			}
		}
		
		System.out.println("Done");
	}
	
}
