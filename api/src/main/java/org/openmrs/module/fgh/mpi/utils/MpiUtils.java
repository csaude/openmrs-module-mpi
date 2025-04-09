package org.openmrs.module.fgh.mpi.utils;

import static org.openmrs.module.fgh.mpi.utils.MpiConstants.MODULE_ID;
import static org.openmrs.module.fgh.mpi.utils.MpiConstants.PATIENT_ID_OFFSET_FILE;
import static org.openmrs.util.OpenmrsUtil.getApplicationDataDirectory;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.openmrs.EncounterType;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.fgh.mpi.entity.InitialLoadTaskController;
import org.openmrs.module.fgh.mpi.integ.MpiContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate5.SessionFactoryUtils;

/**
 * Contains utility methods
 */
public class MpiUtils {
	
	private static final Logger log = LoggerFactory.getLogger(MpiUtils.class);
	
	private static File patientIdOffsetFile;
	
	/**
	 * Gets the file used to store the id of the patient that was last submitted to the MPI
	 *
	 * @return File object
	 */
	private static File getPatientIdOffsetFile() {
		if (patientIdOffsetFile == null) {
			Path path = Paths.get(getApplicationDataDirectory(), MODULE_ID, PATIENT_ID_OFFSET_FILE);
			log.info("Patient Id off set file -> " + path);
			patientIdOffsetFile = path.toFile();
		}
		
		return patientIdOffsetFile;
	}
	
	/**
	 * Gets the patient id of the last submitted patient id
	 *
	 * @return patient id
	 */
	public static Integer getLastSubmittedPatientId() {
		log.info("Loading the patient id of the patient that was last submitted to the MPI");
		
		try {
			File file = getPatientIdOffsetFile();
			String patientId = null;
			if (file.exists()) {
				patientId = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
			}
			
			if (StringUtils.isBlank(patientId)) {
				log.info("No patient id found that was previously saved");
				return null;
			}
			
			log.info("Found id of the patient that was last submitted to the MPI: " + patientId);
			
			return Integer.valueOf(patientId);
		}
		catch (IOException e) {
			throw new APIException("Failed to read the id of the patient that was last submitted to the MPI", e);
		}
	}
	
	/**
	 * Gets the patient id of the last submitted patient id
	 *
	 * @param patientId patient id
	 */
	public static void saveLastSubmittedPatientId(Integer patientId) {
		log.info("Saving the id of the patient that was last submitted to the MPI as: " + patientId);
		
		try {
			FileUtils.writeStringToFile(getPatientIdOffsetFile(), patientId.toString(), StandardCharsets.UTF_8);
			
			log.info("Successfully saved the id of the patient that was last submitted to the MPI as: " + patientId);
		}
		catch (IOException e) {
			log.error("Failed to save the id of the patient that was last submitted to the MPI as: " + patientId, e);
		}
	}
	
	/**
	 * Deletes the file used to store the id of the patient that was last submitted to the MPI
	 */
	public static void deletePatientIdOffsetFile() {
		log.info("Deleting the patient id off set file");
		
		File file = getPatientIdOffsetFile();
		if (!file.exists()) {
			log.info("No patient id off set file found to delete");
			return;
		}
		
		try {
			FileUtils.forceDelete(file);
			
			log.info("Successfully deleted the patient id off set file");
		}
		catch (IOException e) {
			log.error("Failed to delete the patient id off set file", e);
		}
	}
	
	/**
	 * Executes the specified query
	 *
	 * @param query the query to execute
	 * @return results
	 * @throws SQLException
	 */
	public static List<List<Object>> executeQuery(String query) {
		List<List<Object>> results = new ArrayList();
		
		try (Connection conn = getDataSource().getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
			try (ResultSet resultSet = stmt.executeQuery()) {
				ResultSetMetaData rmd = resultSet.getMetaData();
				int columnCount = rmd.getColumnCount();
				
				while (resultSet.next()) {
					List<Object> rowObjects = new ArrayList<>();
					for (int x = 1; x <= columnCount; x++) {
						rowObjects.add(resultSet.getObject(x));
					}
					results.add(rowObjects);
				}
			}
		}
		catch (SQLException e) {
			throw new DAOException(e);
		}
		
		return results;
	}
	
	public static List<Integer> executePatientQuery(String query, Object... params) {
		List<Integer> patientsId = new ArrayList<>();
		
		try (Connection conn = getDataSource().getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
			
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					patientsId.add(rs.getInt("patient_id"));
				}
			}
			return patientsId;

		}
		catch (SQLException e) {
			throw new DAOException("Error querying patients", e);
		}
	}
	
	/**
	 * Retrieves the value of a global property with the specified name
	 *
	 * @param gpName the global property name
	 * @return the global property value
	 */
	public static String getGlobalPropertyValue(String gpName) {
		String value = Context.getAdministrationService().getGlobalProperty(gpName);
		if (StringUtils.isBlank(value)) {
			throw new APIException("No value set for the global property named: " + gpName);
		}
		
		return value;
	}
	
	/**
	 * Gets the encounter type matching the uuid defined as the value of the specified global property
	 * name.
	 *
	 * @param gpName the global property name
	 * @return the encounter type
	 */
	public static EncounterType getEncounterTypeByGlobalProperty(String gpName) {
		final String encTypeUuid = MpiUtils.getGlobalPropertyValue(gpName);
		EncounterType type = Context.getEncounterService().getEncounterTypeByUuid(encTypeUuid);
		if (type == null) {
			throw new APIException("No encounter found matching uuid: " + encTypeUuid);
		}
		
		return type;
	}
	
	/**
	 * Gets the DataSource object
	 *
	 * @return javax.sql.DataSource object
	 */
	private static DataSource getDataSource() {
		return SessionFactoryUtils.getDataSource(Context.getRegisteredComponents(SessionFactory.class).get(0));
	}
	
	public static HttpURLConnection openConnection(String url) throws IOException {
		return (HttpURLConnection) new URL(url).openConnection();
	}
	
	public static HttpURLConnection openConnectionForSSL(String url, MpiContext mpiContext) throws IOException {
		HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
		connection.setSSLSocketFactory(mpiContext.getSslContext().getSocketFactory());
		return connection;
	}
	
	/**
	 * @see Paths#get(String, String...)
	 */
	public static Path createPath(String parent, String... additionalPaths) {
		return Paths.get(parent, additionalPaths);
	}
	
	public static InitialLoadTaskController fetchInitialLoadTaskController() throws SQLException {
		
		try (Connection conn = getDataSource().getConnection();
		        PreparedStatement stmt = conn
		                .prepareStatement("select * from mpi_initial_load_task_controller where is_running = true")) {
			
			try (ResultSet rs = stmt.executeQuery()) {
				InitialLoadTaskController initialLoadTaskController = new InitialLoadTaskController();
				while (rs.next()) {
					initialLoadTaskController.setId(rs.getInt("id"));
					initialLoadTaskController.setActive(rs.getBoolean("is_active"));
					initialLoadTaskController.setPatientOffsetId(rs.getInt("patient_offset_id"));
					initialLoadTaskController.setStartDate(rs.getTimestamp("start_date"));
					initialLoadTaskController.setEndDate(rs.getTimestamp("end_date"));
					initialLoadTaskController.setRunning(rs.getBoolean("is_running"));
				}
				
				if (initialLoadTaskController.getStartDate() == null) {
					return null;
				}
				
				return initialLoadTaskController;
				
			}
		}
		catch (SQLException e) {
			throw new DAOException("Error querying patients", e);
		}
	}
	
	public static void createInitialLoadTaskController(InitialLoadTaskController controller) {
		String query = "INSERT INTO mpi_initial_load_task_controller (is_active, patient_offset_id, start_date, is_running) "
		        + "VALUES (?, ?, ?, ?)";
		
		try (Connection conn = getDataSource().getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
			
			stmt.setBoolean(1, controller.isActive());
			stmt.setInt(2, controller.getPatientOffsetId());
			stmt.setTimestamp(3, new java.sql.Timestamp(controller.getStartDate().getTime()));
			stmt.setBoolean(4, controller.isRunning());
			
			stmt.executeUpdate();
			
		}
		catch (SQLException e) {
			throw new DAOException("Error creating InitialLoadTaskController", e);
		}
	}
	
	public static void updateInitialLoadTaskController(InitialLoadTaskController controller) {
		String query = "UPDATE mpi_initial_load_task_controller SET "
		        + "is_active = ?, patient_offset_id = ?, start_date = ?, end_date = ?, is_running = ? " + " WHERE id = ?";
		
		try (Connection conn = getDataSource().getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
			
			stmt.setBoolean(1, controller.isActive());
			stmt.setInt(2, controller.getPatientOffsetId());
			stmt.setTimestamp(3, new java.sql.Timestamp(controller.getStartDate().getTime()));
			stmt.setTimestamp(4,
			    controller.getEndDate() != null ? new java.sql.Timestamp(controller.getEndDate().getTime()) : null);
			stmt.setBoolean(5, controller.isRunning());
			stmt.setInt(6, controller.getId());
			
			int rowsUpdated = stmt.executeUpdate();
			
			if (rowsUpdated == 0) {
				throw new DAOException("No InitialLoadTaskController record found with id " + controller.getId());
			}
			
		}
		catch (SQLException e) {
			throw new DAOException("Error updating InitialLoadTaskController", e);
		}
	}
	
}
