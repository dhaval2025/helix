/**
 * Copyright (C) 2012 LinkedIn Inc <opensource@linkedin.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.helix.mock.router;

import java.util.List;

import org.I0Itec.zkclient.IDefaultNameSpace;
import org.I0Itec.zkclient.ZkServer;

import com.linkedin.helix.HelixManager;
import com.linkedin.helix.HelixManagerFactory;
import com.linkedin.helix.InstanceType;
import com.linkedin.helix.ZNRecord;
import com.linkedin.helix.manager.zk.ZNRecordSerializer;
import com.linkedin.helix.model.InstanceConfig;
import com.linkedin.helix.spectator.RoutingTableProvider;
import com.linkedin.helix.tools.ClusterSetup;
import com.linkedin.helix.util.HelixUtil;

/**
 * A MockRouter process to demonstrate the integration with cluster manager.
 * This uses Zookeeper in local mode and runs at port 2188
 *
 * @author kgopalak
 *
 */
public class MockRouterProcess
{
  private static final int port = 2188;
  static long runId = System.currentTimeMillis();
  private static final String dataDir = "/tmp/zkDataDir-" + runId;

  private static final String logDir = "/tmp/zkLogDir-" + runId;

  static String clusterName = "mock-cluster-" + runId;

  static String zkConnectString = "localhost:2188";

  private final RoutingTableProvider _routingTableProvider;
  private static ZkServer zkServer;

  public MockRouterProcess()
  {
    _routingTableProvider = new RoutingTableProvider();
  }

  public static void main(String[] args) throws Exception
  {
    setup();
    zkServer.getZkClient().setZkSerializer(new ZNRecordSerializer());
    ZNRecord record = zkServer.getZkClient().readData(
        HelixUtil.getIdealStatePath(clusterName, "TestDB"));

    String externalViewPath = HelixUtil.getExternalViewPath(clusterName, "TestDB");

    MockRouterProcess process = new MockRouterProcess();
    process.start();
    //try to route, there is no master or slave available
    process.routeRequest("TestDB", "TestDB_1");

    //update the externalview on zookeeper
    zkServer.getZkClient().createPersistent(externalViewPath,record);
    //sleep for sometime so that the ZK Callback is received.
    Thread.sleep(1000);
    process.routeRequest("TestDB", "TestDB_1");
    System.exit(1);
  }

  private static void setup()
  {

    IDefaultNameSpace defaultNameSpace = new IDefaultNameSpace()
    {
      @Override
      public void createDefaultNameSpace(org.I0Itec.zkclient.ZkClient client)
      {
        client.deleteRecursive("/" + clusterName);

      }
    };

    zkServer = new ZkServer(dataDir, logDir, defaultNameSpace, port);
    zkServer.start();
    ClusterSetup clusterSetup = new ClusterSetup(zkConnectString);
    clusterSetup.setupTestCluster(clusterName);
    try
    {
      Thread.sleep(1000);
    } catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }

  public void routeRequest(String database, String partition)
  {
    List<InstanceConfig> masters;
    List<InstanceConfig> slaves;
    masters = _routingTableProvider.getInstances(database, partition, "MASTER");
    if (masters != null && !masters.isEmpty())
    {
      System.out.println("Available masters to route request");
      for (InstanceConfig config : masters)
      {
        System.out.println("HostName:" + config.getHostName() + " Port:"
            + config.getPort());
      }
    } else
    {
      System.out.println("No masters available to route request");
    }
    slaves = _routingTableProvider.getInstances(database, partition, "SLAVE");
    if (slaves != null && !slaves.isEmpty())
    {
      System.out.println("Available slaves to route request");
      for (InstanceConfig config : slaves)
      {
        System.out.println("HostName:" + config.getHostName() + " Port:"
            + config.getPort());
      }
    } else
    {
      System.out.println("No slaves available to route request");
    }
  }

  public void start()
  {

    try
    {
      HelixManager manager = HelixManagerFactory.getZKHelixManager(clusterName,
                                                                         null,
                                                                         InstanceType.SPECTATOR,
                                                                         zkConnectString);


      manager.connect();
      manager.addExternalViewChangeListener(_routingTableProvider);
    } catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
