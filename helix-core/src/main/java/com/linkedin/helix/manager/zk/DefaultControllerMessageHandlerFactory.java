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
package com.linkedin.helix.manager.zk;

import org.apache.log4j.Logger;

import com.linkedin.helix.HelixException;
import com.linkedin.helix.NotificationContext;
import com.linkedin.helix.messaging.handling.HelixTaskResult;
import com.linkedin.helix.messaging.handling.MessageHandler;
import com.linkedin.helix.messaging.handling.MessageHandlerFactory;
import com.linkedin.helix.model.Message;
import com.linkedin.helix.model.Message.MessageType;

public class DefaultControllerMessageHandlerFactory implements
    MessageHandlerFactory
{
  private static Logger _logger = Logger.getLogger(DefaultControllerMessageHandlerFactory.class);
  @Override
  public MessageHandler createHandler(Message message,
      NotificationContext context)
  {
    String type = message.getMsgType();
    
    if(!type.equals(getMessageType()))
    {
      throw new HelixException("Unexpected msg type for message "+message.getMsgId()
          +" type:" + message.getMsgType());
    }
    
    return new DefaultControllerMessageHandler(message, context);
  }

  @Override
  public String getMessageType()
  {
    return MessageType.CONTROLLER_MSG.toString();
  }

  @Override
  public void reset()
  {

  }
  
  public static class DefaultControllerMessageHandler extends MessageHandler
  {
    public DefaultControllerMessageHandler(Message message,
        NotificationContext context)
    {
      super(message, context);
    }

    @Override
    public HelixTaskResult handleMessage() throws InterruptedException
    {
      String type = _message.getMsgType();
      HelixTaskResult result = new HelixTaskResult();
      if(!type.equals(MessageType.CONTROLLER_MSG.toString()))
      {
        throw new HelixException("Unexpected msg type for message "+_message.getMsgId()
            +" type:" + _message.getMsgType());
      }
      result.getTaskResultMap().put("ControllerResult", "msg "+ _message.getMsgId() + " from "+_message.getMsgSrc() + " processed");
      result.setSuccess(true);
      return result;
    }

    @Override
    public void onError(Exception e, ErrorCode code, ErrorType type)
    {
      _logger.error("Message handling pipeline get an exception. MsgId:" + _message.getMsgId(), e);
    }
  }
}
