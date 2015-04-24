package se.bitcraze.crazyflielib.toc;


/**
 * An element in the TOC
 *
 */
public class TocElement {

    public static int RW_ACCESS = 1;
    public static int RO_ACCESS = 0;

    private int mIdent = 0;
    private String mGroup = "";
    private String mName = "";
    private VariableType mCtype;
    private int mAccess = RO_ACCESS;

    public TocElement() {

    }

    public int getIdent() {
        return mIdent;
    }

    public void setIdent(int ident) {
        this.mIdent = ident;
    }

    public String getGroup() {
        return mGroup;
    }

    public void setGroup(String group) {
        this.mGroup = group;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getCompleteName() {
        return mGroup + "." + mName;
    }

    public VariableType getCtype() {
        return mCtype;
    }

    public void setCtype(VariableType ctype) {
        this.mCtype = ctype;
    }

    public int getAccess() {
        return mAccess;
    }

    public void setAccess(int access) {
        this.mAccess = access;
    }

    public String getReadableAccess() {
        return (getAccess() == RO_ACCESS) ? "RO" : "RW";
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + this.getGroup() + "." + this.getName() + " (" + this.getIdent() + ", " + this.getCtype() + ", " + this.getReadableAccess() + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mAccess;
        result = prime * result + ((mCtype == null) ? 0 : mCtype.hashCode());
        result = prime * result + ((mGroup == null) ? 0 : mGroup.hashCode());
        result = prime * result + ((mName == null) ? 0 : mName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TocElement)) {
            return false;
        }
        TocElement other = (TocElement) obj;
        if (mAccess != other.mAccess) {
            return false;
        }
        if (mCtype != other.mCtype) {
            return false;
        }
        if (mGroup == null) {
            if (other.mGroup != null) {
                return false;
            }
        } else if (!mGroup.equals(other.mGroup)) {
            return false;
        }
        if (mName == null) {
            if (other.mName != null) {
                return false;
            }
        } else if (!mName.equals(other.mName)) {
            return false;
        }
        return true;
    }

}
