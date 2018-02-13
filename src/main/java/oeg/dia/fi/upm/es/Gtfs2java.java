package oeg.dia.fi.upm.es;

import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class Gtfs2java {

    private  final Logger _log = LoggerFactory.getLogger(Gtfs2java.class);

    public static GtfsDaoImpl read(String path) throws IOException {
        GtfsReader reader = new GtfsReader();
        reader.setInputLocation(new File(path));
        GtfsDaoImpl store = new GtfsDaoImpl();
        reader.setEntityStore(store);
        reader.run();
        return store;
    }
}
