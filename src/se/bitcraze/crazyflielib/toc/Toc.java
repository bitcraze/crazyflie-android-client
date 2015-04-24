package se.bitcraze.crazyflielib.toc;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for TocElements
 */
public class Toc {

    final Logger mLogger = LoggerFactory.getLogger("Toc");

    private Map<String, TocElement> mTocElementMap = new HashMap<String, TocElement>();

    public Toc() {
    }

    /**
     * Clear the TOC
     */
    public void clear() {
        this.mTocElementMap.clear();
    }

    /**
     * Add a new TocElement to the TOC container
     *
     * @param tocElement
     */
    public void addElement(TocElement tocElement) {
        if (tocElement.getGroup().isEmpty()) {
            mLogger.warn("TocElement has no group!");
            return;
        }
        mTocElementMap.put(tocElement.getCompleteName(), tocElement);
    }

    /**
     * Get a TocElement element identified by complete name from the container.
     *
     * @param completeName
     * @return
     */
    public TocElement getElementByCompleteName(String completeName) {
        return mTocElementMap.get(completeName);
    }

    /**
     * Get the TocElement element id-number of the element with the supplied name.
     *
     * @param completeName
     * @return
     */
    public int getElementId(String completeName) {
        TocElement tocElement= mTocElementMap.get(completeName);
        if(tocElement != null) {
            return tocElement.getIdent();
        }
        mLogger.warn("Unable to find TOC element for complete name '" + completeName + "'");
        return -1;
    }

    /**
     * Get a TocElement element identified by name and group from the container
     *
     * @param group
     * @param name
     * @return
     */
    public TocElement getElement(String group, String name) {
        return getElementByCompleteName(group + "." + name);
    }

    /**
     * Get a TocElement element identified by index number from the container
     *
     * @param ident
     * @return
     */
    public TocElement getElementById(int ident) {
        for(TocElement tocElement : mTocElementMap.values()) {
            if (tocElement.getIdent() == ident) {
                return tocElement;
            }
        }
        mLogger.warn("Unable to find TOC element with ID " + ident);
        return null;
    }

    /**
     * Get TocElements as list sorted by ID
     *
     * @return list of TocElements sorted by ID
     */
    //TODO: generate list not every time
    public List<TocElement> getElements() {
        List<TocElement> tocElementList = new ArrayList<TocElement>();
        for (int i = 0; i < getTocSize(); i++) {
            tocElementList.add(getElementById(i));
        }
        return tocElementList;
    }

    public Map<String, TocElement> getTocElementMap() {
        return mTocElementMap;
    }

    public void setTocElementMap(Map<String, TocElement> map) {
        this.mTocElementMap = map;
    }

    public int getTocSize() {
        return mTocElementMap.size();
    }
}
