package ws.wamp.jawampa.android.auth.client;

import com.google.gson.Gson;
import ws.wamp.jawampa.android.WampMessages.AuthenticateMessage;
import ws.wamp.jawampa.android.WampMessages.ChallengeMessage;

public interface ClientSideAuthentication
{
	String getAuthMethod();

	AuthenticateMessage handleChallenge( ChallengeMessage message, Gson gson );
}
