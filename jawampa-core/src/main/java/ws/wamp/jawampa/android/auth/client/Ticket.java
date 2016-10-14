package ws.wamp.jawampa.android.auth.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ws.wamp.jawampa.android.WampMessages.AuthenticateMessage;
import ws.wamp.jawampa.android.WampMessages.ChallengeMessage;

public class Ticket implements ClientSideAuthentication
{
	public static final String AUTH_METHOD = "ticket";

	private final String ticket;

	public Ticket( String ticket )
	{
		this.ticket = ticket;
	}

	@Override
	public String getAuthMethod()
	{
		return AUTH_METHOD;
	}

	@Override
	public AuthenticateMessage handleChallenge( ChallengeMessage message, Gson gson )
	{
		return new AuthenticateMessage( ticket, new JsonObject() );
	}
}
