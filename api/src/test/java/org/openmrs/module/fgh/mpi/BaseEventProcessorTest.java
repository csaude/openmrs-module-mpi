package org.openmrs.module.fgh.mpi;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.openmrs.api.context.Context.getRegisteredComponents;

import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.api.context.Context;
import org.openmrs.module.debezium.DatabaseEvent;
import org.openmrs.util.PrivilegeConstants;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
@SuppressStaticInitializationFor("org.apache.log4j.xml.DOMConfigurator")
public class BaseEventProcessorTest {
	
	public class MockProcessor extends BaseEventProcessor {
		
		MockProcessor(boolean snapshotOnly) {
			super(snapshotOnly);
		}
		
		@Override
		public void process(DatabaseEvent event) {
			
		}
	}
	
	@Mock
	private PatientAndPersonEventHandler mockPersonHandler;
	
	@Mock
	private AssociationEventHandler mockAssociationHandler;
	
	@Mock
	protected RelationshipEventHandler mockRelationshipHandler;
	
	@Mock
	private MpiHttpClient mockClient;
	
	@Mock
	private Map mockFhirResource;
	
	private MockProcessor processor;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		when(getRegisteredComponents(PatientAndPersonEventHandler.class)).thenReturn(singletonList(mockPersonHandler));
		when(getRegisteredComponents(MpiHttpClient.class)).thenReturn(singletonList(mockClient));
	}
	
	@After
	public void tearDown() {
		PowerMockito.verifyStatic(Context.class);
		Context.openSession();
		PowerMockito.verifyStatic(Context.class);
		Context.addProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
		PowerMockito.verifyStatic(Context.class);
		Context.addProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
		PowerMockito.verifyStatic(Context.class);
		Context.addProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
		PowerMockito.verifyStatic(Context.class);
		Context.addProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
		PowerMockito.verifyStatic(Context.class);
		Context.addProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIP_TYPES);
		PowerMockito.verifyStatic(Context.class);
		Context.addProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
		PowerMockito.verifyStatic(Context.class);
		Context.addProxyPrivilege(PrivilegeConstants.GET_ENCOUNTER_TYPES);
		
		PowerMockito.verifyStatic(Context.class);
		Context.removeProxyPrivilege(PrivilegeConstants.SQL_LEVEL_ACCESS);
		PowerMockito.verifyStatic(Context.class);
		Context.removeProxyPrivilege(PrivilegeConstants.GET_PERSON_ATTRIBUTE_TYPES);
		PowerMockito.verifyStatic(Context.class);
		Context.removeProxyPrivilege(PrivilegeConstants.GET_IDENTIFIER_TYPES);
		PowerMockito.verifyStatic(Context.class);
		Context.removeProxyPrivilege(PrivilegeConstants.GET_LOCATIONS);
		PowerMockito.verifyStatic(Context.class);
		Context.removeProxyPrivilege(PrivilegeConstants.GET_RELATIONSHIP_TYPES);
		PowerMockito.verifyStatic(Context.class);
		Context.removeProxyPrivilege(PrivilegeConstants.GET_PATIENTS);
		PowerMockito.verifyStatic(Context.class);
		Context.removeProxyPrivilege(PrivilegeConstants.GET_ENCOUNTER_TYPES);
		PowerMockito.verifyStatic(Context.class);
		Context.closeSession();
	}
	
	private DatabaseEvent createEvent(String table) {
		return new DatabaseEvent(null, table, null, null, null, null);
	}
	
	@Test
	public void createFhirResource_shouldCallPersonHandlerForAPersonEvent() throws Throwable {
		DatabaseEvent event = createEvent("person");
		when(mockPersonHandler.handle(event)).thenReturn(mockFhirResource);
		processor = new MockProcessor(true);
		Assert.assertEquals(mockFhirResource, processor.createFhirResource(event));
		Mockito.verify(mockPersonHandler).handle(event);
		
	}
	
	@Test
	public void createFhirResource_shouldCallPersonHandlerForAPatientEvent() throws Throwable {
		DatabaseEvent event = createEvent("patient");
		when(mockPersonHandler.handle(event)).thenReturn(mockFhirResource);
		processor = new MockProcessor(true);
		Assert.assertEquals(mockFhirResource, processor.createFhirResource(event));
		Mockito.verify(mockPersonHandler).handle(event);
		
	}
	
	@Test
	public void createFhirResource_shouldCallAssociationHandlerForAPersonNameEvent() throws Throwable {
		when(getRegisteredComponents(AssociationEventHandler.class)).thenReturn(singletonList(mockAssociationHandler));
		when(getRegisteredComponents(RelationshipEventHandler.class)).thenReturn(singletonList(mockRelationshipHandler));
		DatabaseEvent event = createEvent("person_name");
		when(mockAssociationHandler.handle(event)).thenReturn(mockFhirResource);
		processor = new MockProcessor(false);
		Assert.assertEquals(mockFhirResource, processor.createFhirResource(event));
		Mockito.verify(mockAssociationHandler).handle(event);
		
	}
	
	@Test
	public void createFhirResource_shouldCallAssociationHandlerForAPersonAttributeEvent() throws Throwable {
		when(getRegisteredComponents(AssociationEventHandler.class)).thenReturn(singletonList(mockAssociationHandler));
		when(getRegisteredComponents(RelationshipEventHandler.class)).thenReturn(singletonList(mockRelationshipHandler));
		DatabaseEvent event = createEvent("person_attribute");
		when(mockAssociationHandler.handle(event)).thenReturn(mockFhirResource);
		processor = new MockProcessor(false);
		Assert.assertEquals(mockFhirResource, processor.createFhirResource(event));
		Mockito.verify(mockAssociationHandler).handle(event);
		
	}
	
	@Test
	public void createFhirResource_shouldCallAssociationHandlerForAPersonAddressEvent() throws Throwable {
		when(getRegisteredComponents(AssociationEventHandler.class)).thenReturn(singletonList(mockAssociationHandler));
		when(getRegisteredComponents(RelationshipEventHandler.class)).thenReturn(singletonList(mockRelationshipHandler));
		DatabaseEvent event = createEvent("person_address");
		when(mockAssociationHandler.handle(event)).thenReturn(mockFhirResource);
		processor = new MockProcessor(false);
		Assert.assertEquals(mockFhirResource, processor.createFhirResource(event));
		Mockito.verify(mockAssociationHandler).handle(event);
		
	}
	
	@Test
	public void createFhirResource_shouldCallAssociationHandlerForAPatientIdentifierEvent() throws Throwable {
		when(getRegisteredComponents(AssociationEventHandler.class)).thenReturn(singletonList(mockAssociationHandler));
		when(getRegisteredComponents(RelationshipEventHandler.class)).thenReturn(singletonList(mockRelationshipHandler));
		DatabaseEvent event = createEvent("patient_identifier");
		when(mockAssociationHandler.handle(event)).thenReturn(mockFhirResource);
		processor = new MockProcessor(false);
		Assert.assertEquals(mockFhirResource, processor.createFhirResource(event));
		Mockito.verify(mockAssociationHandler).handle(event);
		
	}
	
	@Test
	public void createFhirResource_shouldCallAssociationHandlerForAnEncounterEvent() throws Throwable {
		when(getRegisteredComponents(AssociationEventHandler.class)).thenReturn(singletonList(mockAssociationHandler));
		when(getRegisteredComponents(RelationshipEventHandler.class)).thenReturn(singletonList(mockRelationshipHandler));
		DatabaseEvent event = createEvent("encounter");
		when(mockAssociationHandler.handle(event)).thenReturn(mockFhirResource);
		processor = new MockProcessor(false);
		Assert.assertEquals(mockFhirResource, processor.createFhirResource(event));
		Mockito.verify(mockAssociationHandler).handle(event);
		
	}
	
	@Test
	public void createFhirResource_shouldCallRelationshipHandlerForAnRelationshipEvent() throws Throwable {
		when(getRegisteredComponents(AssociationEventHandler.class)).thenReturn(singletonList(mockAssociationHandler));
		when(getRegisteredComponents(RelationshipEventHandler.class)).thenReturn(singletonList(mockRelationshipHandler));
		DatabaseEvent event = createEvent("relationship");
		when(mockRelationshipHandler.handle(event)).thenReturn(mockFhirResource);
		processor = new MockProcessor(false);
		Assert.assertEquals(mockFhirResource, processor.createFhirResource(event));
		Mockito.verify(mockRelationshipHandler).handle(event);
	}
	
}
