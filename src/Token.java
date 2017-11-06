import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.Collections;


public class Token implements java.io.Serializable, UserToken {

    /**
     *
     */
    private static final long serialVersionUID = 234998633654324L;
    private String issuer;
    private String subject;
    private ArrayList<String> groups = new ArrayList<String>();
    private byte[] signature;

    // my_gs.name, subject, my_gs.userList.getUserGroups(subject)
    public Token(String issuer, String subject, ArrayList<String> groups) {
        this.issuer = issuer;
        this.subject = subject;
        this.groups = groups;
        this.signature = null;
    }

    /**
     * This method should return a string describing the issuer of
     * this token.  This string identifies the group server that
     * created this token.  For instance, if "Alice" requests a token
     * from the group server "Server1", this method will return the
     * string "Server1".
     *
     * @return The issuer of this token
     *
     */
    public String getIssuer() {
        return issuer;
    }


    /**
     * This method should return a string indicating the name of the
     * subject of the token.  For instance, if "Alice" requests a
     * token from the group server "Server1", this method will return
     * the string "Alice".
     *
     * @return The subject of this token
     *
     */
    public String getSubject() {
        return subject;
    }


    /**
     * This method extracts the list of groups that the owner of this
     * token has access to.  If "Alice" is a member of the groups "G1"
     * and "G2" defined at the group server "Server1", this method
     * will return ["G1", "G2"].
     *
     * @return The list of group memberships encoded in this token
     *
     */
    public ArrayList<String> getGroups() {
        return groups;
    }

    /**
     * Dump token contents into a basic string
     *
     * @return     String representation of the token
     */
    public String stringify() {
        StringJoiner allGroups = new StringJoiner(",", subject + "," + issuer + ";", "");
        Collections.sort(groups);
        for (String group: groups) {
            allGroups.add(group);
        }
        return allGroups.toString();
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getSignature() {
        return this.signature;
    }

}
