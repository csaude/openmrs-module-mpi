package org.openmrs.module.fgh.mpi;

import static org.openmrs.module.fgh.mpi.MpiConstants.DATETIME_FORMATTER;
import static org.openmrs.module.fgh.mpi.MpiConstants.MODULE_ID;
import static org.openmrs.module.fgh.mpi.MpiConstants.MYSQL_DATETIME_FORMATTER;
import static org.openmrs.module.fgh.mpi.MpiConstants.MYSQL_DATETIME_FORMATTER_MILLS;
import static org.openmrs.module.fgh.mpi.MpiConstants.PATIENT_ID_OFFSET_FILE;
import static org.openmrs.module.fgh.mpi.MpiIntegrationProcessor.ID_PLACEHOLDER;
import static org.openmrs.util.OpenmrsUtil.getApplicationDataDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Contains utility methods
 */
public class MpiUtils {
	
	private static final Logger log = LoggerFactory.getLogger(MpiUtils.class);
	
	public final static String ATTR_TYPE_ID_PLACEHOLDER = "{ATTR_TYPE_ID}";
	
	public final static String ID_QUERY = "SELECT i.identifier, t.uuid, i.uuid, i.identifier_type FROM patient_identifier i, "
	        + "patient_identifier_type t WHERE i.identifier_type = t.patient_identifier_type_id AND i.patient_id " + "= "
	        + ID_PLACEHOLDER + " AND i.voided = 0 ORDER BY preferred DESC";
	
	public final static String NAME_QUERY = "SELECT prefix, given_name, middle_name, family_name, uuid FROM person_name "
	        + "WHERE " + "person_id = " + ID_PLACEHOLDER + " AND voided = 0 ORDER BY preferred DESC";
	
	public final static String ADDRESS_QUERY = "SELECT address1, address2, address3, address5, address6, "
	        + "county_district, state_province, country, start_date, end_date, uuid FROM person_address WHERE "
	        + "person_id = " + ID_PLACEHOLDER + " AND voided = 0 ORDER BY preferred DESC";
	
	public final static String ATTR_QUERY = "SELECT value, uuid FROM person_attribute WHERE person_id = " + ID_PLACEHOLDER
	        + " AND person_attribute_type_id = " + ATTR_TYPE_ID_PLACEHOLDER + " AND voided = 0";
	
	private static String sourceIdUri;
	
	private static File patientIdOffsetFile;
	
	/**
	 * Builds a map of fields and values with patient details that can be serialized as a fhir json
	 * message. This method looks up the up to date patient details from the DB bypassing any hibernate
	 * caches which might be outdated since DB sync is operating outside of the OpenMRS API.
	 * 
	 * @param id the patient id
	 * @param patient a list of column values from the patient table
	 * @param person a list of column values from the person table
	 * @param mpiPatient a map of patient fields and values from the MPI
	 * @return field and value map of the patient details
	 * @throws Exception
	 */
	public static Map<String, Object> buildFhirPatient(String id, List<Object> patient, List<Object> person,
	        Map<String, Object> mpiPatient) throws Exception {
		
		Map<String, Object> fhirRes = new HashMap();
		fhirRes.put(MpiConstants.FIELD_RESOURCE_TYPE, "Patient");
		
		fhirRes.put(MpiConstants.FIELD_ACTIVE, !Boolean.valueOf(patient.get(0).toString()));
		
		AdministrationService adminService = Context.getAdministrationService();
		SessionFactory sf = Context.getRegisteredComponents(SessionFactory.class).get(0);
		DataSource ds = ((SessionFactoryImpl) sf).getConnectionProvider().unwrap(DataSource.class);
		String fhirGender = null;
		String gender = person.get(0) != null ? person.get(0).toString() : null;
		if ("M".equalsIgnoreCase(gender)) {
			fhirGender = "male";
		} else if ("F".equalsIgnoreCase(gender)) {
			fhirGender = "female";
		} else if ("O".equalsIgnoreCase(gender)) {
			fhirGender = "other";
		} else if (StringUtils.isBlank(gender)) {
			fhirGender = "unknown";
		} else if (gender != null) {
			throw new APIException("Don't know how to represent in fhir gender value: " + gender);
		}
		
		fhirRes.put(MpiConstants.FIELD_GENDER, fhirGender);
		
		String birthDate = person.get(1) != null ? person.get(1).toString() : null;
		fhirRes.put(MpiConstants.FIELD_BIRTHDATE, birthDate);
		
		String dead = person.get(2).toString();
		if (Boolean.valueOf(dead)) {
			String deathDateStr = person.get(3) != null ? person.get(3).toString() : null;
			if (StringUtils.isBlank(deathDateStr)) {
				fhirRes.put(MpiConstants.FIELD_DECEASED, dead);
			} else {
				Date deathDate;
				try {
					deathDate = MYSQL_DATETIME_FORMATTER.parse(deathDateStr);
				}
				catch (Exception e) {
					if (log.isDebugEnabled()) {
						log.debug("Failed to parse date '" + deathDateStr + "' for person with id: " + id);
					}
					
					deathDate = MYSQL_DATETIME_FORMATTER_MILLS.parse(deathDateStr);
				}
				fhirRes.put(MpiConstants.FIELD_DECEASED_DATE, DATETIME_FORMATTER.format(deathDate));
			}
		} else {
			fhirRes.put(MpiConstants.FIELD_DECEASED, Boolean.valueOf(dead));
			fhirRes.put(MpiConstants.FIELD_DECEASED_DATE, null);
		}
		
		fhirRes.put(MpiConstants.FIELD_IDENTIFIER, getIds(id, person, mpiPatient, ds));
		fhirRes.put(MpiConstants.FIELD_NAME, getNames(id, mpiPatient, ds));
		fhirRes.put(MpiConstants.FIELD_ADDRESS, getAddresses(id, mpiPatient, ds));
		fhirRes.put(MpiConstants.FIELD_TELECOM, getPhones(id, mpiPatient, adminService, ds));
		
		return fhirRes;
	}
	
	/**
	 * Generates and returns the patient identifier list
	 * 
	 * @param patientId id the patient id
	 * @param person a list of column values from the person table
	 * @param mpiPatient a map of patient fields and values from the MPI
	 * @param ds {@link DataSource} object
	 * @return
	 */
	private static List<Map<String, Object>> getIds(String patientId, List<Object> person, Map<String, Object> mpiPatient,
	        DataSource ds) {
		
		List<List<Object>> idRows = executeQuery(ds, ID_QUERY.replace(ID_PLACEHOLDER, patientId));
		int idListLength = idRows.size() + 1;
		if (mpiPatient != null && mpiPatient.get(MpiConstants.FIELD_IDENTIFIER) != null) {
			idListLength = ((List) mpiPatient.get(MpiConstants.FIELD_IDENTIFIER)).size();
		}
		
		List<Map<String, Object>> identifiers = new ArrayList(idListLength);
		Map<String, Object> sourceIdRes = new HashMap();
		sourceIdRes.put(MpiConstants.FIELD_SYSTEM, getSourceIdUri());
		sourceIdRes.put(MpiConstants.FIELD_VALUE, person.get(4));
		identifiers.add(sourceIdRes);
		
		idRows.stream().forEach(idRow -> {
			Map<String, Object> idResource = new HashMap();
			idResource.put(MpiConstants.FIELD_ID, idRow.get(2));
			String system = MpiConstants.SYSTEM_PREFIX + idRow.get(1);
			idResource.put(MpiConstants.FIELD_SYSTEM, system);
			idResource.put(MpiConstants.FIELD_VALUE, idRow.get(0));
			identifiers.add(idResource);
		});
		
		//We need to overwrite all ids at all indices for an existing patient otherwise OpenCR(hapi fhir) will write 
		//each identifier data by matching indices of our list with the existing list which will be problematic if our
		//list and existing list are of different length
		while (identifiers.size() < idListLength) {
			identifiers.add(null);
		}
		
		return identifiers;
	}
	
	/**
	 * Generates and returns the patient name list
	 *
	 * @param patientId id the patient id
	 * @param mpiPatient a map of patient fields and values from the MPI
	 * @param ds {@link DataSource} object
	 * @return
	 */
	private static List<Map<String, Object>> getNames(String patientId, Map<String, Object> mpiPatient, DataSource ds) {
		
		List<List<Object>> nameRows = executeQuery(ds, NAME_QUERY.replace(ID_PLACEHOLDER, patientId));
		int nameListLength = nameRows.size();
		if (mpiPatient != null && mpiPatient.get(MpiConstants.FIELD_NAME) != null) {
			nameListLength = ((List) mpiPatient.get(MpiConstants.FIELD_NAME)).size();
		}
		
		List<Map<String, Object>> names = new ArrayList(nameListLength);
		boolean foundPreferred = false;
		for (List<Object> nameRow : nameRows) {
			Map<String, Object> nameRes = new HashMap();
			nameRes.put(MpiConstants.FIELD_ID, nameRow.get(4));
			nameRes.put(MpiConstants.FIELD_PREFIX, nameRow.get(0));
			
			List<Object> givenNames = new ArrayList(2);
			givenNames.add(nameRow.get(1));
			givenNames.add(nameRow.get(2));
			
			nameRes.put(MpiConstants.FIELD_GIVEN, givenNames);
			nameRes.put(MpiConstants.FIELD_FAMILY, nameRow.get(3));
			//TODO Add family name suffix and degree as suffix fields
			
			nameRes.put(MpiConstants.FIELD_USE, !foundPreferred ? MpiConstants.USE_OFFICIAL : null);
			if (!foundPreferred) {
				foundPreferred = true;
			}
			
			names.add(nameRes);
		}
		
		while (names.size() < nameListLength) {
			names.add(null);
		}
		
		return names;
	}
	
	/**
	 * Generates and returns the patient address list
	 *
	 * @param patientId id the patient id
	 * @param mpiPatient a map of patient fields and values from the MPI
	 * @param ds {@link DataSource} object
	 * @return
	 */
	private static List<Map<String, Object>> getAddresses(String patientId, Map<String, Object> mpiPatient, DataSource ds)
	        throws Exception {
		
		List<List<Object>> addressRows = executeQuery(ds, ADDRESS_QUERY.replace(ID_PLACEHOLDER, patientId));
		int addressListLength = addressRows.size();
		if (mpiPatient != null && mpiPatient.get(MpiConstants.FIELD_ADDRESS) != null) {
			addressListLength = ((List) mpiPatient.get(MpiConstants.FIELD_ADDRESS)).size();
		}
		
		List<Map<String, Object>> addresses = new ArrayList(addressListLength);
		for (List<Object> addressRow : addressRows) {
			Map<String, Object> addressResource = new HashMap();
			addressResource.put(MpiConstants.FIELD_ID, addressRow.get(10));
			List<Object> lineRes = new ArrayList(5);
			lineRes.add(addressRow.get(1));//address2
			lineRes.add(addressRow.get(4));//address6
			lineRes.add(addressRow.get(3));//address5
			lineRes.add(addressRow.get(2));//address3
			lineRes.add(addressRow.get(0));//address1
			addressResource.put(MpiConstants.FIELD_LINE, lineRes);
			addressResource.put(MpiConstants.FIELD_DISTRICT, addressRow.get(5));
			addressResource.put(MpiConstants.FIELD_STATE, addressRow.get(6));
			addressResource.put(MpiConstants.FIELD_COUNTRY, addressRow.get(7));
			
			Map<String, Object> period = new HashMap();
			String startDate = null;
			if (addressRow.get(8) != null && StringUtils.isNotBlank(addressRow.get(8).toString())) {
				startDate = DATETIME_FORMATTER.format(MYSQL_DATETIME_FORMATTER.parse(addressRow.get(8).toString()));
			}
			
			String endDate = null;
			if (addressRow.get(9) != null && StringUtils.isNotBlank(addressRow.get(9).toString())) {
				endDate = DATETIME_FORMATTER.format(MYSQL_DATETIME_FORMATTER.parse(addressRow.get(9).toString()));
			}
			
			period.put(MpiConstants.FIELD_START, startDate);
			period.put(MpiConstants.FIELD_END, endDate);
			addressResource.put(MpiConstants.FIELD_PERIOD, period);
			
			addresses.add(addressResource);
		}
		
		while (addresses.size() < addressListLength) {
			addresses.add(null);
		}
		
		return addresses;
	}
	
	/**
	 * Generates and returns the patient phone number list
	 *
	 * @param patientId id the patient id
	 * @param mpiPatient a map of patient fields and values from the MPI
	 * @param as {@link AdministrationService} object
	 * @param ds {@link DataSource} object
	 * @return
	 */
	private static List<Map<String, Object>> getPhones(String patientId, Map<String, Object> mpiPatient,
	        AdministrationService as, DataSource ds) {
		PersonService personService = Context.getPersonService();
		String attTypeUuid = as.getGlobalProperty(MpiConstants.GP_PHONE_MOBILE);
		String attTypeId = personService.getPersonAttributeTypeByUuid(attTypeUuid).getId().toString();
		String phoneQuery = ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, attTypeId);
		List<List<Object>> phoneRows = executeQuery(ds, phoneQuery);
		Map<String, Object> phoneResource = null;
		if (!phoneRows.isEmpty()) {
			phoneResource = new HashMap();
			phoneResource.put(MpiConstants.FIELD_ID, phoneRows.get(0).get(1));
			phoneResource.put(MpiConstants.FIELD_SYSTEM, MpiConstants.PHONE);
			phoneResource.put(MpiConstants.FIELD_VALUE, phoneRows.get(0).get(0));
			phoneResource.put(MpiConstants.FIELD_USE, MpiConstants.MOBILE);
		}
		
		int phoneListLength = 2;
		if (mpiPatient != null && mpiPatient.get(MpiConstants.FIELD_TELECOM) != null) {
			phoneListLength = ((List) mpiPatient.get(MpiConstants.FIELD_TELECOM)).size();
		}
		List<Map<String, Object>> phones = new ArrayList(phoneListLength);
		phones.add(phoneResource);
		
		attTypeUuid = as.getGlobalProperty(MpiConstants.GP_PHONE_HOME);
		attTypeId = personService.getPersonAttributeTypeByUuid(attTypeUuid).getId().toString();
		phoneQuery = ATTR_QUERY.replace(ID_PLACEHOLDER, patientId).replace(ATTR_TYPE_ID_PLACEHOLDER, attTypeId);
		phoneRows = executeQuery(ds, phoneQuery);
		phoneResource = null;
		if (!phoneRows.isEmpty()) {
			phoneResource = new HashMap();
			phoneResource.put(MpiConstants.FIELD_ID, phoneRows.get(0).get(1));
			phoneResource.put(MpiConstants.FIELD_SYSTEM, MpiConstants.PHONE);
			phoneResource.put(MpiConstants.FIELD_VALUE, phoneRows.get(0).get(0));
			phoneResource.put(MpiConstants.FIELD_USE, MpiConstants.HOME);
		}
		
		phones.add(phoneResource);
		
		while (phones.size() < phoneListLength) {
			phones.add(null);
		}
		
		return phones;
	}
	
	/**
	 * Gets the source id uri by reading the value of global property value
	 * {@link MpiConstants#GP_SOURCE_ID_URI}
	 * 
	 * @return source id uri
	 */
	public static String getSourceIdUri() {
		if (sourceIdUri == null) {
			sourceIdUri = Context.getAdministrationService().getGlobalProperty(MpiConstants.GP_SOURCE_ID_URI);
		}
		
		if (StringUtils.isBlank(sourceIdUri)) {
			throw new APIException(MpiConstants.GP_SOURCE_ID_URI + " global property value is not set");
		}
		
		return sourceIdUri;
	}
	
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
				log.info("Not patient id found that was previously saved");
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
	 * @return patient id
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
	 * Executes the specified query against the specified {@link DataSource}
	 * 
	 * @param ds {@link DataSource} object
	 * @param query the query to execute
	 * @return results
	 * @throws SQLException
	 */
	public static List<List<Object>> executeQuery(DataSource ds, String query) {
		List<List<Object>> results = new ArrayList();
		
		try (Connection conn = ds.getConnection(); PreparedStatement statement = conn.prepareStatement(query)) {
			try (ResultSet resultSet = statement.executeQuery()) {
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
	
}
