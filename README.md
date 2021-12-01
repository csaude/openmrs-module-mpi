# MPI Integration Module
Provides a mechanism to integrate OpenMRS with OpenCR, the module tracks changes in an OpenMRS database i.e. inserts, 
updates and deletes using the debezium module. This allows the module to be able to create or update a patient's record 
in OpenCR.

### Technical Overview

### Logging


### Assumptions
- You have a patient identifier type used to capture NIDs and that every patient has been assigned an NID
- You have a running instance of OpenCR
- You have created a client certificate for the OpenMRS instance(Central) where the module will be deployed, the 
  certificate will be required to communicate with OpenCR.

### Build and Install
The MPI module depends on the [debezium module](https://github.com/FriendsInGlobalHealth/openmrs-module-debezium.git) 
therefore you need to first clone, build and install the debezium module as described [here](https://github.com/FriendsInGlobalHealth/openmrs-module-debezium#build-and-install)
Then you can clone, build and install this module as shown below,
```
git clone https://github.com/FriendsInGlobalHealth/openmrs-module-mpi.git
cd openmrs-module-mpi
mvn clean install
```
Take the generated .omod file in the `omod/target` folder and install it in the central OpenMRS instance

### OpenMRS Configuration

#### Global properties
Navigate to the main admin settings page as mentioned below,
- From the main menu, click **Administration**
- Under the **Maintenance** section, click on **Settings**, click on the **Mpi** link in the left panel, and you
  should see a page like the screenshot below, please make sure to read the description of each property carefully.

![Module Settings](docs/settings_screenshot.png)
#### Logging
- Navigate back to the main admin settings page as mentioned below,
- From the main menu, click **Administration**
- Under the **Maintenance** section, click on **Settings**, click on the **Log** link in the left panel, update the 
  value of the **Level**(log.level) global property and append `org.openmrs.module.debezium:info,org.openmrs.module.fgh.mpi:info`
  
Note that the module captures its logs and those of the debezium module and writes them to a special log file located 
in the OpenMRS application data directory at `/path-to-app-data-directory/mpi/logs/mpi.log`, this can be very useful when 
debugging any issues that may arise with the module.

### OpenCR Installation
[OpenCR](https://intrahealth.github.io/client-registry/) is the master patient(client registry) implementation we are 
using for this integration, the steps below are based on [system admin documentation](https://intrahealth.github.io/client-registry/admin/configuration/) and 
[server installation](https://intrahealth.github.io/client-registry/admin/installation_full/).
- Clone the repository below into a directory of choice.
  ```
  git https://github.com/FriendsInGlobalHealth/centralization-docker-setup
  cd centralization-docker-setup/opencr
  ```
- Create a copy of the `docker-compose.prod.yml` file and name it docker-compose.yml
- Edit the `db.env` file and be sure to change at least the passwords and any other values to match your requirements.
- Edit the `application.yml` file and make sure that the spring datasource username and password match those defined in 
  the `db.env` file, you can also tweak more field values to match your requirements.

#### Update Config File  
Open the `config_production.json` and make the following changes,
- Change the oauth secret, it is set via the `auth`.`secret` field, note that we are using dot notation to refer to 
  nested fields i.e. `secret` is a field of the root `auth` field. 
- The configuration file has a `systems` field with existing entries where NID is one of them although the URI 
  contains an incorrect UUID for the NID patient identifier type from your OpenMRS instance, so you need to set the 
  correct UUID in its URI and **DO NOT** change its key name.
- You also need to add entries for all the other OpenMRS identifier types to the systems field for display purposes, 
  below is an example entry,
  
  ```
  "nid": {
      "uri": "urn:uuid:15a3fd64-1d5f-11e0-b929-000c29ad1d08",
      "displayName": "NID"
    }
  ```
  
  **nid**: Logical key name for the entry
  
  **uri**: The system URI, it MUST match the associated patient identifier type UUID in OpenMRS prefixed with `urn:uuid:`
  
  **displayName**: The display label for the identifier type in the OpenCR UI when viewing a patient record

**IMPORTANT:** 
- In your new docker-compose.yml file for the elastic search service, it's highly recommended to make sure that volume
  mapped to the elastic data directory is backed up regularly at least once a day, more would even be better if your 
  resources can allow.
- Please remember to also regularly backup the MySQL database defined in `docker-compose.yml` which is used by the hapi 
  fhir server to store all records pushed to OpenCR.

#### Patient Matching Configuration
The production docker compose template comes with a built-in patient matching configuration defined in the 
`decisionRule.json` file with the following rules,
- **Rule 1**: Any 2 or more patients with the same NID, gender, date of birth, given and family name should be auto 
  merged while those the same and any of the other fields don't match are suggested as potential matches.
- **Rule 2**: Any 2 or more patients with the same NID, gender, date of birth, given and family name should be auto 
  merged while those with the same gender and birthdate and then any of the nid or given or family fields match are 
  suggested as potential matches. 

For more details on configuring patient matching in OpenCR refer to the resources below,
- [OpenCR patient matching documentation](https://intrahealth.github.io/client-registry/)
- Inline documentation in the [Deterministic matching template](https://github.com/intrahealth/client-registry/server/config/decisionRulesDeterministicTemplate.json)
- Inline documentation in the [Probabilistic matching template](https://github.com/intrahealth/client-registry/server/config/decisionRulesProbabilisticTemplate.json)

### Initial Loading Of Existing Patients

### Incremental Integration

