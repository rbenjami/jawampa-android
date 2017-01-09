/*
 * Copyright 2014 Matthias Einwag
 *
 * The jawampa authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package ws.wamp.jawampa.android.transport.netty;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import ws.wamp.jawampa.android.ApplicationError;
import ws.wamp.jawampa.android.WampSerialization;
import ws.wamp.jawampa.android.connection.IWampClientConnectionConfig;
import ws.wamp.jawampa.android.connection.IWampConnector;
import ws.wamp.jawampa.android.connection.IWampConnectorProvider;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Returns factory methods for the establishment of WAMP connections between
 * clients and routers.<br>
 */
public class NettyWampClientConnectorProvider implements IWampConnectorProvider
{
	@Override
	public ScheduledExecutorService createScheduler()
	{
		NioEventLoopGroup scheduler = new NioEventLoopGroup( 1, new ThreadFactory()
		{
			@Override
			public Thread newThread( Runnable r )
			{
				Thread t = new Thread( r, "WampClientEventLoop" );
				t.setDaemon( true );
				return t;
			}
		} );
		return scheduler;
	}

	@Override
	public IWampConnector createConnector( URI uri, IWampClientConnectionConfig configuration, List<WampSerialization> serializations ) throws Exception
	{
		String scheme = uri.getScheme();
		scheme = scheme != null ? scheme : "";

		// Check if the configuration is a netty configuration.
		// However null is an allowed value
		NettyWampConnectionConfig nettyConfig;
		if ( configuration instanceof NettyWampConnectionConfig )
			nettyConfig = (NettyWampConnectionConfig) configuration;
		else if ( configuration != null )
			throw new ApplicationError( ApplicationError.INVALID_CONNECTION_CONFIGURATION );
		else
			nettyConfig = null;

		if ( !scheme.equalsIgnoreCase( "ws" ) && !scheme.equalsIgnoreCase( "wss" ) )
			throw new ApplicationError( ApplicationError.INVALID_URI );

		// Check the host and port field for validity
		if ( uri.getHost() == null || uri.getPort() == 0 )
			throw new ApplicationError( ApplicationError.INVALID_URI );

		// Initialize SSL when required
		boolean needSsl = uri.getScheme().equalsIgnoreCase( "wss" );
		SslContext sslCtx0 = getSslContext( needSsl, nettyConfig );

		String subProtocols = WampSerialization.makeWebsocketSubprotocolList( serializations );

		int maxFramePayloadLength = ( nettyConfig == null ) ? NettyWampConnectionConfig.DEFAULT_MAX_FRAME_PAYLOAD_LENGTH : nettyConfig.getMaxFramePayloadLength();

		// Return a factory that creates a channel for websocket connections
		return new NettyWampClientConnector( sslCtx0, uri, needSsl, subProtocols, maxFramePayloadLength );
	}

	private SslContext getSslContext( boolean needSsl, NettyWampConnectionConfig nettyConfig ) throws SSLException
	{
		SslContext sslCtx0;
		if ( needSsl && ( nettyConfig == null || nettyConfig.sslContext() == null ) )
		{
			// Create a default SslContext when we got none provided through the constructor
			sslCtx0 = SslContext.newClientContext( InsecureTrustManagerFactory.INSTANCE );
		}
		else if ( needSsl )
			sslCtx0 = nettyConfig.sslContext();
		else
			sslCtx0 = null;
		return sslCtx0;
	}
}
