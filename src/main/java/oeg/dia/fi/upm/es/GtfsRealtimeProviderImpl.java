/**
 * Copyright (C) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package oeg.dia.fi.upm.es;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onebusaway.gtfs.services.GtfsDao;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeExporterModule;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeLibrary;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeMutableProvider;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;

import static java.lang.Math.toIntExact;

/**
 * This class produces GTFS-realtime trip updates and vehicle positions by
 * periodically polling the custom SEPTA vehicle data API and converting the
 * resulting vehicle data into the GTFS-realtime format.
 * 
 * Since this class implements {@link GtfsRealtimeProvider}, it will
 * automatically be queried by the {@link GtfsRealtimeExporterModule} to export
 * the GTFS-realtime feeds to file or to host them using a simple web-server, as
 * configured by the client.
 * 
 * @author bdferris
 * 
 */
@Singleton
public class GtfsRealtimeProviderImpl {

  private static final Logger _log = LoggerFactory.getLogger(GtfsRealtimeProviderImpl.class);

  private ScheduledExecutorService _executor;

  private GtfsRealtimeMutableProvider _gtfsRealtimeProvider;

  private GtfsDao gtfs;

  private Tram2json tram2json;

  private Integer count;

  /**
   * How often vehicle data will be downloaded, in seconds.
   */
  private int _refreshInterval = 30;

  @Inject
  public void setGtfsRealtimeProvider(GtfsRealtimeMutableProvider gtfsRealtimeProvider) {
    _gtfsRealtimeProvider = gtfsRealtimeProvider;
  }

  /**
   * @param url the URL for the SEPTA vehicle data API.
   */


  /**
   * @param refreshInterval how often vehicle data will be downloaded, in
   *          seconds.
   */
  public void setRefreshInterval(int refreshInterval) {
    _refreshInterval = refreshInterval;
  }

  /**
   * The start method automatically starts up a recurring task that periodically
   * downloads the latest vehicle data from the SEPTA vehicle stream and
   * processes them.
   */
  @PostConstruct
  public void start() {
    _log.info("starting GTFS-realtime service");
    _executor = Executors.newSingleThreadScheduledExecutor();
    _executor.scheduleAtFixedRate(new VehiclesRefreshTask(), 0,
        _refreshInterval, TimeUnit.SECONDS);
    tram2json = new Tram2json();
    count=0;
  }

  /**
   * The stop method cancels the recurring vehicle data downloader task.
   */
  @PreDestroy
  public void stop() {
    _log.info("stopping GTFS-realtime service");
    _executor.shutdownNow();
  }

  /****
   * Private Methods - Here is where the real work happens
   ****/

  /**
   * This method downloads the latest vehicle data, processes each vehicle in
   * turn, and create a GTFS-realtime feed of trip updates and vehicle positions
   * as a result.
   */
  private void refreshVehicles() throws IOException, JSONException {

    /**
     * We download the vehicle details as an array of JSON objects.
     */
    HashMap<String,ArrayList<StopTimeAux>> tripsArray = downloadVehicleDetails();
    Integer entity_id=0;
    /**
     * The FeedMessage.Builder is what we will use to build up our GTFS-realtime
     * feeds. We create a feed for both trip updates and vehicle positions.
     */
    FeedMessage.Builder tripUpdates = GtfsRealtimeLibrary.createFeedMessageBuilder();

    /**
     * We iterate over every JSON vehicle object.
     */

    Iterator it = tripsArray.entrySet().iterator();

    while(it.hasNext()) {
      Map.Entry pair = (Map.Entry) it.next();

      String trip_id = (String)pair.getKey();

      /**
       * We construct a TripDescriptor and VehicleDescriptor, which will be used
       * in both trip updates and vehicle positions to identify the trip and
       * vehicle. Ideally, we would have a trip id to use for the trip
       * descriptor, but the SEPTA api doesn't include it, so we settle for a
       * route id instead.
       */
      TripDescriptor.Builder tripDescriptor = TripDescriptor.newBuilder();
      tripDescriptor.setTripId(trip_id);


      /**
       * To construct our TripUpdate, we create a stop-time arrival event for
       * the next stop for the vehicle, with the specified arrival delay. We add
       * the stop-time update to a TripUpdate builder, along with the trip and
       * vehicle descriptors.
       */
      ArrayList<StopTimeAux> stopTimeAux = (ArrayList<StopTimeAux>) pair.getValue();
      stopTimeAux.sort(Comparator.comparing(StopTimeAux::getStop_sequence));
      TripUpdate.Builder tripUpdate = TripUpdate.newBuilder();
      tripUpdate.setTrip(tripDescriptor);

      for(StopTimeAux aux : stopTimeAux) {
        StopTimeEvent.Builder departure = StopTimeEvent.newBuilder();
        departure.setTime(aux.getDepartureTime());
        departure.setDelay(toIntExact(aux.getDelay()));

        StopTimeEvent.Builder arrival = StopTimeEvent.newBuilder();
        arrival.setTime(aux.getArrivalTime());
        arrival.setDelay(toIntExact(aux.getDelay()));

        StopTimeUpdate.Builder stopTimeUpdate = StopTimeUpdate.newBuilder();
        stopTimeUpdate.setDeparture(departure);
        stopTimeUpdate.setArrival(arrival);
        stopTimeUpdate.setStopSequence(aux.getStop_sequence());
        stopTimeUpdate.setStopId(aux.getStop_id());

        tripUpdate.addStopTimeUpdate(stopTimeUpdate);
      }
      /**
       * Create a new feed entity to wrap the trip update and add it to the
       * GTFS-realtime trip updates feed.
       */
      FeedEntity.Builder tripUpdateEntity = FeedEntity.newBuilder();
      tripUpdateEntity.setId(entity_id.toString());
      tripUpdateEntity.setTripUpdate(tripUpdate);
      tripUpdates.addEntity(tripUpdateEntity);
      entity_id++;

    }

    /**
     * Build out the final GTFS-realtime feed messagse and save them.
     */
    _gtfsRealtimeProvider.setTripUpdates(tripUpdates.build());

    _log.info("trips extracted: " + tripUpdates.getEntityCount());
  }

  /**
   * @return a JSON array parsed from the data pulled from the SEPTA vehicle
   *         data API.
   */
  private HashMap<String, ArrayList<StopTimeAux>> downloadVehicleDetails() throws IOException, JSONException {
      if(gtfs==null)
          if(System.getenv("company").equals("tbs"))
              gtfs = Gtfs2java.read("./datasets/tbs");
          else
              gtfs= Gtfs2java.read("./datasets/tbx");

      Json2gtfsrl json2gtfsrl = new Json2gtfsrl(gtfs,tram2json.recolectTimes());
      return json2gtfsrl.joinStaticAndRT();
  }

  /**
   * Task that will download new vehicle data from the remote data source when
   * executed.
   */
  private class VehiclesRefreshTask implements Runnable {

    @Override
    public void run() {
      try {
        _log.info("refreshing vehicles");
        if(count==0){
            tram2json.getKey();
            _log.info("Getting the key");
        }
        else if(count>110){
            count=0;
          }
        count++;
        _log.info("Calls with the same key: "+count);
        refreshVehicles();
      } catch (Exception ex) {
        _log.warn("Error in vehicle refresh task", ex);
      }
    }
  }

}
