import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.PrivateKey;

public abstract class Server {
	
	protected int port;
	public String name;
	private static KeyPair rsaKeys;

	abstract void start();
	
	public Server(int _SERVER_PORT, String _serverName) {
		port = _SERVER_PORT;
		name = _serverName;
		rsaKeys = null;
	}
	
		
	public int getPort() {
		return port;
	}
	
	public String getName() {
		return name;
	}

	public PublicKey getPublicKey() {
		return this.rsaKeys.getPublic();
	}

	protected PrivateKey getPrivateKey() {
		return this.rsaKeys.getPrivate();
	}

	protected void setKeys(KeyPair newKeys) {
		this.rsaKeys = newKeys;
	}

}
