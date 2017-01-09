package ws.wamp.jawampa.android.transport.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import ws.wamp.jawampa.android.WampMessages;
import ws.wamp.jawampa.android.WampSerialization;
import ws.wamp.jawampa.android.connection.IWampConnection;
import ws.wamp.jawampa.android.connection.IWampConnectionPromise;

public class NettyWampClientConnection implements IWampConnection
{
	private ChannelHandlerContext ctx;
	private WampSerialization serialization;

	public NettyWampClientConnection( ChannelHandlerContext ctx, WampSerialization serialization )
	{
		this.ctx = ctx;
		this.serialization = serialization;
	}


	@Override
	public WampSerialization serialization()
	{
		return serialization;
	}

	@Override
	public boolean isSingleWriteOnly()
	{
		return false;
	}

	@Override
	public void sendMessage( WampMessages.WampMessage message, final IWampConnectionPromise<Void> promise )
	{
		ChannelFuture f = ctx.writeAndFlush( message );
		f.addListener( new ChannelFutureListener()
		{
			@Override
			public void operationComplete( ChannelFuture future ) throws Exception
			{
				if ( future.isSuccess() || future.isCancelled() )
					promise.fulfill( null );
				else
					promise.reject( future.cause() );
			}
		} );
	}

	@Override
	public void close( boolean sendRemaining, final IWampConnectionPromise<Void> promise )
	{
		// sendRemaining is ignored. Remaining data is always sent
		ctx.writeAndFlush( Unpooled.EMPTY_BUFFER ).addListener( new ChannelFutureListener() {
			@Override
			public void operationComplete( ChannelFuture future ) throws Exception
			{
				future.channel().close().addListener( new ChannelFutureListener() {
					@Override
					public void operationComplete( ChannelFuture future ) throws Exception
					{
						if ( future.isSuccess() || future.isCancelled() )
							promise.fulfill( null );
						else
							promise.reject( future.cause() );
					}
				} );
			}
		} );
	}
}
