package ws.wamp.jawampa.android.transport.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ws.wamp.jawampa.android.ApplicationError;
import ws.wamp.jawampa.android.WampMessages;
import ws.wamp.jawampa.android.WampSerialization;
import ws.wamp.jawampa.android.connection.IPendingWampConnectionListener;
import ws.wamp.jawampa.android.connection.IWampConnectionListener;

public class NettyWampClientChannelHandler extends SimpleChannelInboundHandler<WampMessages.WampMessage>
{
	private boolean connectionWasEstablished = false;

	/** Guard to prevent forwarding events after the channel was closed */
	private boolean wasClosed = false;

	private IPendingWampConnectionListener connectListener;
	private IWampConnectionListener        connectionListener;

	public NettyWampClientChannelHandler( IPendingWampConnectionListener connectListener, IWampConnectionListener connectionListener )
	{
		this.connectListener = connectListener;
		this.connectionListener = connectionListener;
	}

	@Override
	public void channelInactive( ChannelHandlerContext ctx ) throws Exception
	{
		if ( wasClosed )
			return;
		wasClosed = true;
		if ( connectionWasEstablished )
			connectionListener.transportClosed();
		else // The transport closed before the websocket handshake was completed
			connectListener.connectFailed( new ApplicationError( ApplicationError.TRANSPORT_CLOSED ) );
	}

	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception
	{
		if ( wasClosed )
			return;
		wasClosed = true;
		if ( connectionWasEstablished )
			connectionListener.transportError( cause );
		else // The transport closed before the websocket handshake was completed
			connectListener.connectFailed( cause );
		super.exceptionCaught( ctx, cause );
	}

	@Override
	public void userEventTriggered( final ChannelHandlerContext ctx, Object evt ) throws Exception
	{
		if ( wasClosed )
			return;
		if ( evt instanceof ConnectionEstablishedEvent )
		{
			ConnectionEstablishedEvent ev = (ConnectionEstablishedEvent) evt;
			WampSerialization serialization = ev.serialization();

			connectionWasEstablished = true;

			// Connection to the remote host was established
			// However the WAMP session is not established until the handshake was finished
			connectListener.connectSucceeded( new NettyWampClientConnection( ctx, serialization ) );
		}
	}

	@Override
	protected void channelRead0( ChannelHandlerContext ctx, WampMessages.WampMessage msg ) throws Exception
	{
		if ( wasClosed )
			return;
		if ( !connectionWasEstablished )
			throw new AssertionError();
		connectionListener.messageReceived( msg );
	}
}
