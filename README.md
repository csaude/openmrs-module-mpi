# MPI Integration Module
Provides a mechanism to integrate OpenMRS with OpenCR, the module tracks changes in an OpenMRS database i.e. inserts, 
updates and deletes using the debezium module. This allows the module to be able to create or update a patient's record 
in OpenCR.

### Technical Overview

### Logging


### Assumptions
* You have a patient identifier type used to capture NIDs and that every patient has been assigned an NID
* You have a running instance of OpenCR
* You have created a client certificate for the OpenMRS instance(Central) where the module will be deployed, the 
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
* From the main menu, click **Administration**
* Under the **Maintenance** section, click on **Settings**, click on the **Mpi** link in the left panel, and you
  should see a page like the screenshot below, please make sure to read the description of each property carefully.

![Module Settings](docs/settings_screenshot.png)
#### Logging
* Navigate back to the main admin settings page as mentioned below,
* From the main menu, click **Administration**
* Under the **Maintenance** section, click on **Settings**, click on the **Log** link in the left panel, update the 
  value of the **Level**(log.level) global property and append `org.openmrs.module.debezium:info,org.openmrs.module.fgh.mpi:info`
  
Note that the module captures its logs and those of the debezium module and writes them to a special log file located 
in the OpenMRS application data directory at `/path-to-app-data-directory/mpi/logs/mpi.log`, this can be very useful when 
debugging any issues that may arise with the module.

### OpenCR Installation
[OpenCR](https://intrahealth.github.io/client-registry/) is the master patient(client registry) implementation we are 
using for this integration, the steps below are based on [system admin documentation](https://intrahealth.github.io/client-registry/admin/configuration/) and 
[server installation](https://intrahealth.github.io/client-registry/admin/installation_full/).
* Clone the repository below into a directory of choice.
  ```
  git https://github.com/FriendsInGlobalHealth/centralization-docker-setup
  cd centralization-docker-setup/opencr
  ```

### OpenCR Configuration

#### General
* Configure all OpenMRS identifier types in OpenCR for display purposes
* 


#### Patient Matching


### Initial Loading Of Existing Patients

### Incremental Integration

