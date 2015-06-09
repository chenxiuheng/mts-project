/* 
 * Copyright 2012 Devoteam http://www.devoteam.com
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * 
 * This file is part of Multi-Protocol Test Suite (MTS).
 * 
 * Multi-Protocol Test Suite (MTS) is free software: you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License.
 * 
 * Multi-Protocol Test Suite (MTS) is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Multi-Protocol Test Suite (MTS).
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.devoteam.srit.xmlloader.radius;


import com.devoteam.srit.xmlloader.core.ThreadPool;
import com.devoteam.srit.xmlloader.core.exception.ExecutionException;
import com.devoteam.srit.xmlloader.core.log.GlobalLogger;
import com.devoteam.srit.xmlloader.core.log.TextEvent;
import com.devoteam.srit.xmlloader.core.protocol.Channel;
import com.devoteam.srit.xmlloader.core.protocol.Msg;
import com.devoteam.srit.xmlloader.core.protocol.StackFactory;
import gp.net.radius.RadiusSocket;
import gp.net.radius.data.IdentifierHandler;
import gp.net.radius.data.RadiusMessage;
import gp.utils.arrays.Array;
import gp.utils.arrays.ReadOnlyDefaultArray;


public class ChannelRadius extends Channel implements Runnable
{
    private RadiusSocket radiusSocket;

    private Array secret;

    private IdentifierHandler identifierHandler;

    /** Creates a new instance of Channel */
    public ChannelRadius(String name, String aLocalHost, String aLocalPort, String aRemoteHost, String aRemotePort, String aProtocol, String secret) throws Exception
    {
        super(name, aLocalHost, aLocalPort, aRemoteHost, aRemotePort, aProtocol);

        this.secret = new ReadOnlyDefaultArray(secret.getBytes());

        this.identifierHandler = new IdentifierHandler();
    }

    public Array getSecret()
    {
        return this.secret;
    }

    public IdentifierHandler getIdentifierHandler()
    {
        return this.identifierHandler;
    }

    public synchronized boolean close()
    {
        if(null != radiusSocket)
        {
            this.radiusSocket.close();
            this.radiusSocket = null;
        }
        return true;
    }

    public synchronized boolean open() throws Exception
    {
        if(null == this.getLocalHost())
        {
            this.radiusSocket = new RadiusSocket(this.getLocalPort());
        }
        else
        {
            this.radiusSocket = new RadiusSocket(this.getLocalHost(), this.getLocalPort());
        }

        this.setLocalPort(this.radiusSocket.getLocalPort());
        int bufferSize = StackFactory.getStack(StackFactory.PROTOCOL_RADIUS).getConfig().getInteger("radius.RECEIVE_BUFFER_LENGTH", 4096);
        this.radiusSocket.setBufferSize(bufferSize);

        ThreadPool.reserve().start(this);
        return true;
    }

    public synchronized boolean sendMessage(Msg msg) throws Exception
    {
        MsgRadius msgRadius = (MsgRadius) msg;
        RadiusMessage radiusMessage = msgRadius.getRadiusMessage();

        GlobalLogger.instance().getApplicationLogger().debug(TextEvent.Topic.PROTOCOL , "ChannelRadius, send message\n", msgRadius);

        // use the message remote address if present, else, the channel's one.
        if (null != radiusMessage.getRemoteAddress())
        {
            this.radiusSocket.send(radiusMessage);
        }
        else
        {
            throw new ExecutionException("ChannelRadius : there is no remotePort or remoteHost to send this message to");
        }

        return true;
    }

    /** Get the transport protocol of this message */
    public String getTransport()
    {
    	return StackFactory.PROTOCOL_UDP;
    }

    public void run()
    {
        while (this.radiusSocket != null && this.radiusSocket.isOpen())
        {
            try
            {
                RadiusMessage radiusMessage = this.radiusSocket.receive();
                GlobalLogger.instance().getApplicationLogger().debug(TextEvent.Topic.PROTOCOL, "ChannelRadius, received a radius message");

                radiusMessage.setSecret(this.secret);

                MsgRadius msgRadius = new MsgRadius(radiusMessage);
                this.setRemoteHost(radiusMessage.getRemoteAddress().getAddress().getHostAddress());
                this.setRemotePort(radiusMessage.getRemoteAddress().getPort());
                msgRadius.setChannel(this);

                if (!msgRadius.isRequest())
                {
                    GlobalLogger.instance().getApplicationLogger().debug(TextEvent.Topic.PROTOCOL, "ChannelRadius, free identifier ", radiusMessage.getIdentifier());
                    this.identifierHandler.freeIdentifier(radiusMessage.getIdentifier());
                }

                if (1 == radiusMessage.getCode())
                {
                    radiusMessage.decodeUserPasswordAvps();
                }

                if (msgRadius.hasValidAuthenticator())
                {
                    GlobalLogger.instance().getApplicationLogger().debug(TextEvent.Topic.PROTOCOL, "ChannelRadius, send radius message to stack");
                    StackFactory.getStack(StackFactory.PROTOCOL_RADIUS).receiveMessage(msgRadius);
                }
                else
                {
                    GlobalLogger.instance().getApplicationLogger().warn(TextEvent.Topic.PROTOCOL, "ChannelRadius, silently discarded message\n", msgRadius);
                }
            }
            catch (Exception e)
            {
                GlobalLogger.instance().getApplicationLogger().warn(TextEvent.Topic.PROTOCOL, e, "Error occured : decoding a message or socket error");
            }
        }

        try
        {
            synchronized(this)
            {
                if(null != this.radiusSocket)
                {
                    GlobalLogger.instance().getApplicationLogger().debug(TextEvent.Topic.PROTOCOL, "ChannelRadius : closing ", this.getName());
                    StackFactory.getStack(StackFactory.PROTOCOL_RADIUS).closeChannel(this.getName());
                }
            }
        }
        catch(Exception e)
        {
        	// nothing to do
        }
    }
}
