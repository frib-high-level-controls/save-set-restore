package org.csstudio.saverestore;

/**
 *
 * <code>SearchCriterion</code> defines the criterion by which the snapshots are search for. This can be either the
 * comment, the tag name, the tag message or any combination of these three.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public enum SearchCriterion {
    COMMENT("Snapshot Comment"), TAG_NAME("Snapshot tag name"), TAG_MESSAGE("Snapshot tag message");

    /** The human readable name of the search criterion */
    public final String name;

    private SearchCriterion(String name) {
        this.name = name;
    }
}