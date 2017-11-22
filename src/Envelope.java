import java.util.ArrayList;


public class Envelope implements java.io.Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -7726335089122193103L;
	private String msg;
	private ArrayList<Object> objContents = new ArrayList<Object>();
	private int n;

	public Envelope(String text) {
		msg = text;
	}

	public String getMessage() {
		return msg;
	}

	public ArrayList<Object> getObjContents() {
		return objContents;
	}

	public void addObject(Object object) {
		objContents.add(object);
	}

	public void setN(int n) {
		this.n = n;
	}

	public int getN() {
		return this.n;
	}

}
