package ws.wamp.jawampa.auth.client;

import com.google.gson.Gson;
import ws.wamp.jawampa.WampMessages.AuthenticateMessage;
import ws.wamp.jawampa.WampMessages.ChallengeMessage;

public interface ClientSideAuthentication {
    String getAuthMethod();
    AuthenticateMessage handleChallenge( ChallengeMessage message, Gson gson );
}
