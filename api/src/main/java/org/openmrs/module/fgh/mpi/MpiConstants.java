package org.openmrs.module.fgh.mpi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.Range;

public class MpiConstants {
	
	public final static String MODULE_ID = "mpi";
	
	public final static String[] WATCHED_TABLES = new String[] { "person", "patient", "person_name", "person_address",
	        "patient_identifier", "person_attribute", "relationship" };
	
	public final static String GP_INITIAL = MODULE_ID + ".initial.loading";
	
	public final static String GP_HEALTH_CENTER_EXT_URL = MODULE_ID + ".patient.health.center.url";
	
	public final static String GP_PERSON_UUID_EXT_URL = MODULE_ID + ".person.uuid.url";
	
	public final static String PATIENT_ID_OFFSET_FILE = ".patientIdOffset.txt";
	
	public final static String GP_MPI_BASE_URL = MODULE_ID + ".server.base.url";
	
	public final static String GP_KEYSTORE_PATH = MODULE_ID + ".keystore.path";
	
	public final static String GP_KEYSTORE_PASS = MODULE_ID + ".keystore.password";
	
	public final static String GP_KEYSTORE_TYPE = MODULE_ID + ".keystore.type";
	
	public final static String GP_PHONE_MOBILE = MODULE_ID + ".person.attribute.type.mobile.phone";
	
	public final static String GP_PHONE_HOME = MODULE_ID + ".person.attribute.type.home.phone";
	
	public final static String GP_UUID_SYSTEM = MODULE_ID + ".patient.uuid.system.uri";
	
	public final static String GP_ID_TYPE_SYSTEM_MAP = MODULE_ID + ".identifier.type.system.uri.mappings";
	
	public final static String GP_IDENTIFIER_TYPE_SYSTEM = MODULE_ID + ".identifier.type.terminology.system.uri";
	
	public final static String GP_IDENTIFIER_TYPE_CONCEPT_MAP = MODULE_ID + ".identifier.type.concept.mappings";
	
	public final static String GP_RELATIONSHIP_TYPE_SYSTEM = MODULE_ID + ".relationship.type.terminology.system.uri";
	
	public final static String GP_RELATIONSHIP_TYPE_CONCEPT_MAP_A = MODULE_ID
	        + ".relationship.type.personA.concept.mappings";
	
	public final static String GP_RELATIONSHIP_TYPE_CONCEPT_MAP_B = MODULE_ID
	        + ".relationship.type.personB.concept.mappings";
	
	public final static String GP_OPENMRS_UUID_CONCEPT_MAP = MODULE_ID + ".patient.uuid.concept.map";
	
	public final static String GP_INITIAL_BATCH_SIZE = MODULE_ID + ".initial.loading.batch.size";
	
	public final static String OPENMRS_UUID = "OpenMRS Internal UUID";
	
	public final static String HEALTH_CENTER_ATTRIB_TYPE_UUID = "8d87236c-c2cc-11de-8d13-0010c6dffd0f";
	
	public final static String FIELD_RESOURCE_TYPE = "resourceType";
	
	public final static String FIELD_ID = "id";
	
	public final static String FIELD_IDENTIFIER = "identifier";
	
	public final static String FIELD_ACTIVE = "active";
	
	public final static String FIELD_NAME = "name";
	
	public final static String FIELD_GENDER = "gender";
	
	public final static String FIELD_BIRTHDATE = "birthDate";
	
	public final static String FIELD_DECEASED = "deceasedBoolean";
	
	public final static String FIELD_DECEASED_DATE = "deceasedDateTime";
	
	public final static String FIELD_SYSTEM = "system";
	
	public final static String FIELD_VALUE = "value";
	
	public final static String FIELD_PREFIX = "prefix";
	
	public final static String FIELD_FAMILY = "family";
	
	public final static String FIELD_GIVEN = "given";
	
	public final static String FIELD_ADDRESS = "address";
	
	public final static String FIELD_USE = "use";
	
	public final static String FIELD_LINE = "line";
	
	public final static String FIELD_DISTRICT = "district";
	
	public final static String FIELD_STATE = "state";
	
	public final static String FIELD_COUNTRY = "country";
	
	public final static String FIELD_PERIOD = "period";
	
	public final static String FIELD_START = "start";
	
	public final static String FIELD_END = "end";
	
	public final static String FIELD_TELECOM = "telecom";
	
	public final static String FIELD_TYPE = "type";
	
	public final static String FIELD_ENTRY = "entry";
	
	public final static String FIELD_RESOURCE = "resource";
	
	public final static String FIELD_EXTENSION = "extension";
	
	public final static String FIELD_URL = "url";
	
	public final static String FIELD_VALUE_STR = "valueString";
	
	public final static String FIELD_VALUE_UUID = "valueUuid";
	
	public final static String FIELD_CONTACT = "contact";
	
	public final static String FIELD_RELATIONSHIP = "relationship";
	
	public final static String FIELD_CODING = "coding";
	
	public final static String FIELD_TEXT = "text";
	
	public final static String FIELD_CODE = "code";
	
	public final static String FIELD_DISPLAY = "display";
	
	public final static String PATIENT = "Patient";
	
	public final static String BUNDLE = "Bundle";
	
	public final static String BATCH = "batch";
	
	public final static String HOME = "home";
	
	public final static String MOBILE = "mobile";
	
	public final static String USE_OFFICIAL = "official";
	
	public final static String GENDER_MALE = "male";
	
	public final static String GENDER_FEMALE = "female";
	
	public final static String GENDER_OTHER = "other";
	
	public final static String GENDER_UNKNOWN = "unknown";
	
	public final static String PHONE = "phone";
	
	public final static String IDENTIFIER = "identifier";
	
	public final static String NAME = "name";
	
	public final static String UUID_PREFIX = "urn:uuid:";
	
	public final static DateFormat DATETIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	
	public final static String REQ_PARAM_SOURCE_ID = "sourceIdentifier";
	
	public final static String RESPONSE_FIELD_PARAM = "parameter";
	
	public final static String RESPONSE_FIELD_VALUE_REF = "valueReference";
	
	public final static String GP_SANTE_CLIENT_ID = MODULE_ID + ".sante.db.client.id";
	
	public final static String GP_SANTE_CLIENT_SECRET = MODULE_ID + ".sante.db.client.secret";
	
	public final static String GP_SANTE_LOGIN_TYPE = MODULE_ID + ".sante.db.login.type";
	
	public final static String GP_SANTE_SCOPE = MODULE_ID + ".sante.db.scope";
	
	public final static String GP_SANTE_USERNAME = MODULE_ID + ".sante.db.username";
	
	public final static String GP_SANTE_PASSWORD = MODULE_ID + ".sante.db.password";
	
	public final static String GP_LOAD_SYSTEM = MODULE_ID + ".load.system";
	
	public final static String GP_MPI_APP_CONTENT_TYPE = MODULE_ID + ".application.content.type";
	
	public final static String GP_AUTHENTICATION_TYPE = MODULE_ID + ".authentication.type";
	
	public final static String AUTHENTICATION_OUTH_TYPE = "outh";
	
	public final static String AUTHENTICATION_SSL_TYPE = "ssl";
	
	public final static Range<Integer> HTTP_REQUEST_SUCCESS_RANGE = Range.between(200, 299);
}
