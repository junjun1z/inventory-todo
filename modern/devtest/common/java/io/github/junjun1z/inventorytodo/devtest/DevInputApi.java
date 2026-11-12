package io.github.junjun1z.inventorytodo.devtest;

/** Posts input events through the loader's event system (development only). Each returns true if consumed. */
public interface DevInputApi {

    boolean click(Object screen, double x, double y, int button);

    boolean key(Object screen, int key);

    boolean character(Object screen, char c);
}
