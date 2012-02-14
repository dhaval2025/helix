package com.linkedin.helix;

public class Criteria
{
  public enum DataSource
  {
    IDEALSTATES, EXTERNALVIEW
  }
  /**
   * This can be CONTROLLER, PARTICIPANT, ROUTER Cannot be null
   */
  InstanceType recipientInstanceType;
  /**
   * If true this will only be process by the instance that was running when the
   * message was sent. If the instance process dies and comes back up it will be
   * ignored.
   */
  boolean sessionSpecific;
  /**
   * applicable only in case PARTICIPANT use * to broadcast to all instances
   */
  String instanceName = "";
  /**
   * Name of the resource. Use * to send message to all resources
   * owned by an instance.
   */
  String resourceName = "";
  /**
   * Resource partition. Use * to send message to all partitions of a given
   * resource
   */
  String partitionName = "";
  /**
   * State of the resource
   */
  String partitionState = "";
  /**
   * Exclude sending message to your self. True by default
   */
  boolean selfExcluded = true;
  /**
   * Determine if use external view or ideal state as source of truth
   */
  DataSource _dataSource = DataSource.EXTERNALVIEW;
  
  public DataSource getDataSource()
  {
    return _dataSource;
  }
  
  public void setDataSource(DataSource source)
  {
    _dataSource = source;
  }

  public boolean isSelfExcluded()
  {
    return selfExcluded;
  }

  public void setSelfExcluded(boolean selfExcluded)
  {
    this.selfExcluded = selfExcluded;
  }

  public InstanceType getRecipientInstanceType()
  {
    return recipientInstanceType;
  }

  public void setRecipientInstanceType(InstanceType recipientInstanceType)
  {
    this.recipientInstanceType = recipientInstanceType;
  }

  public boolean isSessionSpecific()
  {
    return sessionSpecific;
  }

  public void setSessionSpecific(boolean sessionSpecific)
  {
    this.sessionSpecific = sessionSpecific;
  }

  public String getInstanceName()
  {
    return instanceName;
  }

  public void setInstanceName(String instanceName)
  {
    this.instanceName = instanceName;
  }

  public String getResourceName()
  {
    return resourceName;
  }

  public void setResource(String resourceName)
  {
    this.resourceName = resourceName;
  }

  public String getPartitionName()
  {
    return partitionName;
  }

  public void setPartition(String partitionName)
  {
    this.partitionName = partitionName;
  }

  public String getPartitionState()
  {
    return partitionState;
  }

  public void setPartitionState(String partitionState)
  {
    this.partitionState = partitionState;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("instanceName").append("=").append(instanceName);
    sb.append("resourceName").append("=").append(resourceName);
    sb.append("partitionName").append("=").append(partitionName);
    sb.append("partitionState").append("=").append(partitionState);
    return sb.toString();
  }

}
