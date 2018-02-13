package oeg.dia.fi.upm.es;


import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.services.GtfsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;


public class Json2gtfsrl{

    private static final Logger _log = LoggerFactory.getLogger(Json2gtfsrl.class);
    private LocalDateTime todayMidnight = LocalDateTime.of(LocalDate.now(ZoneId.of("Europe/Madrid")), LocalTime.MIDNIGHT);
    private GtfsDao gtfs;
    private JSONArray times;
    private HashMap<String,ArrayList<StopTimeAux>> delays;


    public Json2gtfsrl(GtfsDao gtfs, JSONArray times){
        delays = new HashMap<>();
        this.gtfs = gtfs;
        this.times = times;
    }

    public HashMap<String,ArrayList<StopTimeAux>> joinStaticAndRT (){
        JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
        Collection<StopTime> stopTimes = gtfs.getAllStopTimes();
        stopTimes.forEach(stopTime -> {
            Date arrivalTime = Date.from(todayMidnight.plusSeconds(stopTime.getArrivalTime()).toInstant(ZoneOffset.UTC));
            Date departureTime = Date.from(todayMidnight.plusSeconds(stopTime.getDepartureTime()).toInstant(ZoneOffset.UTC));
            if(arrivalTime.getTime()>=System.currentTimeMillis() && arrivalTime.getTime()<=System.currentTimeMillis()+600000) {
                times.forEach(obj -> {

                    JSONObject time = (JSONObject) obj;

                    if (time.getString("lineName").equals(stopTime.getTrip().getId().getId().substring(0, 2))
                        && time.get("code").toString().substring(2, 4).equals(stopTime.getStop().getId().getId()) &&
                        jaroWinklerDistance.apply(time.getString("destination").toLowerCase(), stopTime.getTrip().getTripHeadsign().toLowerCase()) > 0.8) {
                            try {
                                StopTimeAux aux = new StopTimeAux();
                                Date timeDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time.getString("arrivalTime"));
                                if (arrivalTime.getTime() - 120000 < timeDate.getTime() && arrivalTime.getTime() + 120000 > timeDate.getTime()) {
                                    String trip_id=stopTime.getTrip().getId().getId();
                                    Long delay =arrivalTime.getTime() - timeDate.getTime();
                                    aux.setStop_sequence(stopTime.getStopSequence());
                                    aux.setDepartureTime((departureTime.getTime()+delay)/1000);
                                    aux.setArrivalTime((arrivalTime.getTime()+delay)/1000);
                                    aux.setDelay(delay/1000);
                                    aux.setStop_id(stopTime.getStop().getId().getId());
                                    if(!this.delays.containsKey(trip_id)) {
                                        this.delays.put(trip_id, new ArrayList<>());
                                    }
                                    this.delays.get(trip_id).add(aux);
                                }

                        } catch (Exception e) {
                                _log.error("Error joinning the data: "+e.getMessage());
                        }
                    }
                });
            }
        });
        return delays;
    }




}


