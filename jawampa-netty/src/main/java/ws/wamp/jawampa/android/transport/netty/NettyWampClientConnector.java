package ws.wamp.jawampa.android.transport.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import ws.wamp.jawampa.android.ApplicationError;
import ws.wamp.jawampa.android.connection.IPendingWampConnection;
import ws.wamp.jawampa.android.connection.IPendingWampConnectionListener;
import ws.wamp.jawampa.android.connection.IWampConnectionListener;
import ws.wamp.jawampa.android.connection.IWampConnector;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;

public class NettyWampClientConnector implements IWampConnector
{
	private SslContext sslCtx0;
	private URI        uri;
	private boolean    needSsl;
	private String     subProtocols;
	private int        maxFramePayloadLength;

	private ChannelHandler connectionHandler;
	private IPendingWampConnectionListener connectListener;
	private int port;

	public NettyWampClientConnector( SslContext sslCtx0, URI uri, boolean needSsl, String subProtocols, int maxFramePayloadLength )
	{
		this.sslCtx0 = sslCtx0;
		this.uri = uri;
		this.needSsl = needSsl;
		this.subProtocols = subProtocols;
		this.maxFramePayloadLength = maxFramePayloadLength;
	}

	@Override
	public IPendingWampConnection connect( ScheduledExecutorService scheduler, IPendingWampConnectionListener connectListener, IWampConnectionListener connectionListener )
	{
		// Use well-known ports if not explicitly specified
		if ( uri.getPort() == -1 )
		{
			if ( needSsl )
				port = 443;
			else port = 80;
		}
		else
			port = uri.getPort();

		final WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
				uri, WebSocketVersion.V13, subProtocols,
				false, new DefaultHttpHeaders(), maxFramePayloadLength );

		/**
		 * Netty handler for that receives and processes WampMessages and state
		 * events from the pipeline.
		 * A new instance of this is created for each connection attempt.
		 */
		this.connectionHandler = new NettyWampClientChannelHandler( connectListener, connectionListener );

		this.connectListener = connectListener;

		// If the assigned scheduler is a netty eventloop use this
		final EventLoopGroup nettyEventLoop;
		if ( scheduler instanceof EventLoopGroup )
		{
			nettyEventLoop = (EventLoopGroup) scheduler;
		}
		else
		{
			connectListener.connectFailed( new ApplicationError( ApplicationError.INCOMATIBLE_SCHEDULER ) );
			return IPendingWampConnection.Dummy;
		}

		Bootstrap b = new Bootstrap();
		b.group( nettyEventLoop ).channel( NioSocketChannel.class ).handler( new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel( SocketChannel ch )
			{
				ChannelPipeline p = ch.pipeline();
				if ( sslCtx0 != null )
					p.addLast( sslCtx0.newHandler( ch.alloc(), uri.getHost(), port ) );
				p.addLast(
						new HttpClientCodec(),
						new HttpObjectAggregator( 8192 ),
						new WebSocketClientProtocolHandler( handshaker, false ),
						new WebSocketFrameAggregator( WampHandlerConfiguration.MAX_WEBSOCKET_FRAME_SIZE ),
						new WampClientWebsocketHandler( handshaker ),
						connectionHandler );
			}
		} );

		final ChannelFuture connectFuture = b.connect( uri.getHost(), port );
		connectFuture.addListener( new ChannelFutureListener()
		{
			@Override
			public void operationComplete( ChannelFuture future ) throws Exception
			{
				if ( future.isSuccess() )
				{
					// Do nothing. The connection is only successful when the websocket handshake succeeds
				}
				else
				{
					// Remark: Might be called directly in addListener
					// Therefore addListener should be the last call
					// Remark2: This branch will be taken upon cancellation.
					// This is required by the contract.
					NettyWampClientConnector.this.connectListener.connectFailed( future.cause() );
				}
			}
		} );

		// Return the connection in progress with the ability for cancellation
		return new IPendingWampConnection()
		{
			@Override
			public void cancelConnect()
			{
				connectFuture.cancel( false );
			}
		};
	}
}

