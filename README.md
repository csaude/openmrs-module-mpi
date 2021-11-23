# MPI Integration Module
Provides a mechanism to integrate OpenMRS with OpenCR, the module tracks changes in an OpenMRS database i.e. inserts, 
updates and deletes using the debezium module. This allows the module to be able to create or update a patient's record 
in OpenCR.

### Technical Overview


### Assumptions
* You have a running instance of OpenCR
* You have created a client certificate for this OpenMRS instance which will be required to communicate with OpenCR.

### Build and Install
The MPI module depends on the [debezium module](https://github.com/FriendsInGlobalHealth/openmrs-module-debezium.git) 
therefore you need to first clone and build the debezium module as described [here](https://github.com/FriendsInGlobalHealth/openmrs-module-debezium#build-and-install)
Then you can clone and build this module as shown below,
```
git clone https://github.com/FriendsInGlobalHealth/openmrs-module-mpi.git
cd openmrs-module-mpi
mvn clean install
```

### Configuration

#### Setting global properties
Navigate to the main admin settings page as mentioned below,
* From the main menu, click **Administration**
* Under the **Maintenance** section, click on **Settings**, click on the **Mpi** link in the left panel, and you
  should see a page like the screenshot below, please make sure to read the description of each property carefully.

![Module Settings](docs/settings_screenshot.png)

#### Before Using The Module
