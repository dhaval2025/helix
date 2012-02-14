package com.linkedin.helix.controller.stages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.linkedin.helix.PropertyType;
import com.linkedin.helix.ZNRecord;
import com.linkedin.helix.controller.pipeline.StageContext;
import com.linkedin.helix.controller.stages.AttributeName;
import com.linkedin.helix.controller.stages.ClusterEvent;
import com.linkedin.helix.controller.stages.ReadClusterDataStage;
import com.linkedin.helix.controller.stages.ResourceComputationStage;
import com.linkedin.helix.model.CurrentState;
import com.linkedin.helix.model.IdealState;
import com.linkedin.helix.model.LiveInstance;
import com.linkedin.helix.model.Resource;
import com.linkedin.helix.tools.IdealStateCalculatorForStorageNode;

public class TestResourceComputationStage extends BaseStageTest
{
  /**
   * Case where we have one resource in IdealState
   *
   * @throws Exception
   */
  @Test
  public void testSimple() throws Exception
  {
    int nodes = 5;
    List<String> instances = new ArrayList<String>();
    for (int i = 0; i < nodes; i++)
    {
      instances.add("localhost_" + i);
    }
    int partitions = 10;
    int replicas = 1;
    String resourceName = "testResource";
    ZNRecord record = IdealStateCalculatorForStorageNode.calculateIdealState(
        instances, partitions, replicas, resourceName, "MASTER", "SLAVE");
    IdealState idealState = new IdealState(record);
    idealState.setStateModelDefRef("MasterSlave");
    manager.getDataAccessor().setProperty(PropertyType.IDEALSTATES,
                                          idealState,
                                          resourceName);
    ResourceComputationStage stage = new ResourceComputationStage();
    runStage(event, new ReadClusterDataStage());
    runStage(event, stage);

    Map<String, Resource> resource = event
        .getAttribute(AttributeName.RESOURCES.toString());
    AssertJUnit.assertEquals(1, resource.size());

    AssertJUnit.assertEquals(resource.keySet().iterator().next(),
        resourceName);
    AssertJUnit.assertEquals(resource.values().iterator().next()
        .getResourceName(), resourceName);
    AssertJUnit.assertEquals(resource.values().iterator().next()
        .getStateModelDefRef(), idealState.getStateModelDefRef());
    AssertJUnit.assertEquals(resource.values().iterator().next()
        .getPartitions().size(), partitions);
  }

  @Test
  public void testMultipleResources() throws Exception
  {
//    List<IdealState> idealStates = new ArrayList<IdealState>();
    String[] resources = new String[]
        { "testResource1", "testResource2" };
    List<IdealState> idealStates = setupIdealState(5, resources, 10, 1);
    ResourceComputationStage stage = new ResourceComputationStage();
    runStage(event, new ReadClusterDataStage());
    runStage(event, stage);

    Map<String, Resource> resourceMap = event
        .getAttribute(AttributeName.RESOURCES.toString());
    AssertJUnit.assertEquals(resources.length, resourceMap.size());

    for (int i = 0; i < resources.length; i++)
    {
      String resourceName = resources[i];
      IdealState idealState = idealStates.get(i);
      AssertJUnit.assertTrue(resourceMap.containsKey(resourceName));
      AssertJUnit.assertEquals(resourceMap.get(resourceName)
          .getResourceName(), resourceName);
      AssertJUnit.assertEquals(resourceMap.get(resourceName)
          .getStateModelDefRef(), idealState.getStateModelDefRef());
      AssertJUnit.assertEquals(resourceMap.get(resourceName)
          .getPartitions().size(), idealState.getNumPartitions());
    }
  }

  @Test
  public void testMultipleResourcesWithSomeDropped() throws Exception
  {
    int nodes = 5;
    List<String> instances = new ArrayList<String>();
    for (int i = 0; i < nodes; i++)
    {
      instances.add("localhost_" + i);
    }
    String[] resources = new String[]
    { "testResource1", "testResource2" };
    List<IdealState> idealStates = new ArrayList<IdealState>();
    for (int i = 0; i < resources.length; i++)
    {
      int partitions = 10;
      int replicas = 1;
      String resourceName = resources[i];
      ZNRecord record = IdealStateCalculatorForStorageNode
          .calculateIdealState(instances, partitions, replicas,
              resourceName, "MASTER", "SLAVE");
      IdealState idealState = new IdealState(record);
      idealState.setStateModelDefRef("MasterSlave");
      manager.getDataAccessor().setProperty(PropertyType.IDEALSTATES,
                                            idealState,
                                            resourceName);

      idealStates.add(idealState);
    }
    // ADD A LIVE INSTANCE WITH A CURRENT STATE THAT CONTAINS RESOURCE WHICH NO
    // LONGER EXISTS IN IDEALSTATE
    String instanceName = "localhost_" + 3;
    LiveInstance liveInstance = new LiveInstance(instanceName);
    String sessionId = UUID.randomUUID().toString();
    liveInstance.setSessionId(sessionId);
    manager.getDataAccessor().setProperty(PropertyType.LIVEINSTANCES,
                                          liveInstance,
                                          instanceName);

    String oldResource = "testResourceOld";
    CurrentState currentState = new CurrentState(oldResource);
    currentState.setState("testResourceOld_0", "OFFLINE");
    currentState.setState("testResourceOld_1", "SLAVE");
    currentState.setState("testResourceOld_2", "MASTER");
    currentState.setStateModelDefRef("MasterSlave");
    manager.getDataAccessor().setProperty(PropertyType.CURRENTSTATES,
                                          currentState,
                                          instanceName,
                                          sessionId,
                                          oldResource);

    ResourceComputationStage stage = new ResourceComputationStage();
    runStage(event, new ReadClusterDataStage());
    runStage(event, stage);

    Map<String, Resource> resourceMap = event
        .getAttribute(AttributeName.RESOURCES.toString());
    // +1 because it will have one for current state
    AssertJUnit.assertEquals(resources.length + 1, resourceMap.size());

    for (int i = 0; i < resources.length; i++)
    {
      String resourceName = resources[i];
      IdealState idealState = idealStates.get(i);
      AssertJUnit.assertTrue(resourceMap.containsKey(resourceName));
      AssertJUnit.assertEquals(resourceMap.get(resourceName)
          .getResourceName(), resourceName);
      AssertJUnit.assertEquals(resourceMap.get(resourceName)
          .getStateModelDefRef(), idealState.getStateModelDefRef());
      AssertJUnit.assertEquals(resourceMap.get(resourceName)
          .getPartitions().size(), idealState.getNumPartitions());
    }
    // Test the data derived from CurrentState
    AssertJUnit.assertTrue(resourceMap.containsKey(oldResource));
    AssertJUnit.assertEquals(resourceMap.get(oldResource)
        .getResourceName(), oldResource);
    AssertJUnit.assertEquals(resourceMap.get(oldResource)
        .getStateModelDefRef(), currentState.getStateModelDefRef());
    AssertJUnit
        .assertEquals(resourceMap.get(oldResource).getPartitions()
            .size(), currentState.getPartitionStateMap().size());
    AssertJUnit.assertNotNull(resourceMap.get(oldResource).getPartition("testResourceOld_0"));
    AssertJUnit.assertNotNull(resourceMap.get(oldResource).getPartition("testResourceOld_1"));
    AssertJUnit.assertNotNull(resourceMap.get(oldResource).getPartition("testResourceOld_2"));

  }

  @Test
  public void testNull()
  {
    ClusterEvent event = new ClusterEvent("sampleEvent");
    ResourceComputationStage stage = new ResourceComputationStage();
    StageContext context = new StageContext();
    stage.init(context);
    stage.preProcess();
    boolean exceptionCaught = false;
    try
    {
      stage.process(event);
    } catch (Exception e)
    {
      exceptionCaught = true;
    }
    AssertJUnit.assertTrue(exceptionCaught);
    stage.postProcess();
  }


//  public void testEmptyCluster()
//  {
//    ClusterEvent event = new ClusterEvent("sampleEvent");
//    ClusterManager manager = new Mocks.MockManager();
//    event.addAttribute("clustermanager", manager);
//    ResourceComputationStage stage = new ResourceComputationStage();
//    StageContext context = new StageContext();
//    stage.init(context);
//    stage.preProcess();
//    boolean exceptionCaught = false;
//    try
//    {
//      stage.process(event);
//    } catch (Exception e)
//    {
//      exceptionCaught = true;
//    }
//    Assert.assertTrue(exceptionCaught);
//    stage.postProcess();
//  }

}
