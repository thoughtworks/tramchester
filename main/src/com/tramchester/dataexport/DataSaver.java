package com.tramchester.dataexport;

public interface DataSaver<T> {
    void write(T itemToSave);

    void open();

    void close();
}
