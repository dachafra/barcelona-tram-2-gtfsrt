package oeg.dia.fi.upm.es;


import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.services.GtfsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;


public class Json2gtfsrl{

    private static final Logger _log = LoggerFactory.getLogger(Json2gtfsrl.class);
    private ZoneId zoneId= ZoneId.of("Europe/Madrid");
    private LocalDateTime todayMidnight = LocalDateTime.of(LocalDate.now(zoneId), LocalTime.MIDNIGHT);
    private GtfsDao gtfs;
    private JSONArray times;
    private HashMap<String,ArrayList<StopTimeAux>> delays;
    private boolean date;


    public Json2gtfsrl(GtfsDao gtfs, JSONArray times){
        delays = new HashMap<>();
        this.gtfs = gtfs;
        this.times = times;
    }

    public HashMap<String,ArrayList<StopTimeAux>> joinStaticAndRT (){
        JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
        long timeNow = System.currentTimeMillis();
        Collection<StopTime> stopTimes = gtfs.getAllStopTimes();
        stopTimes.forEach(stopTime -> {
            Date arrivalTime = Date.from(todayMidnight.plusSeconds(stopTime.getArrivalTime()).toInstant(ZoneOffset.from(ZonedDateTime.now(zoneId))));
            Date departureTime = Date.from(todayMidnight.plusSeconds(stopTime.getDepartureTime()).toInstant(ZoneOffset.from(ZonedDateTime.now(zoneId))));
            if(arrivalTime.getTime()>=timeNow && arrivalTime.getTime()<=timeNow+600000) {
                times.forEach(obj -> {
                    JSONObject time = (JSONObject) obj;
                    try {
                        Date timeDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time.getString("arrivalTime"));
                        if (timeDate.getTime()>=timeNow && timeDate.getTime()<=timeNow+600000 &&
                            time.getString("lineName").equals(stopTime.getTrip().getId().getId().substring(0, 2))  &&
                            time.get("code").toString().substring(2, 4).equals(stopTime.getStop().getId().getId()) &&
                            jaroWinklerDistance.apply(time.getString("destination").toLowerCase(), stopTime.getTrip().getTripHeadsign().toLowerCase()) > 0.8 &&
                                checkDate(stopTime)) {
                                    StopTimeAux aux = new StopTimeAux();
                                    if (arrivalTime.getTime() - 150000 < timeDate.getTime() && arrivalTime.getTime() + 150000 > timeDate.getTime()) {
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
                        }
                    } catch (Exception e) {
                        _log.error("Error joinning the data: "+e.getMessage());
                    }
                });
            }
        });
        return delays;
    }

    public boolean checkDate(StopTime stopTime){
        LocalDate today = LocalDate.now();
        Date dateToday = Calendar.getInstance().getTime();
        Collection<ServiceCalendar> calendar=gtfs.getAllCalendars();
        Collection<ServiceCalendarDate> calendarDates = gtfs.getAllCalendarDates();
        date = false;

        calendar.forEach(c -> {
            if(c.getServiceId().getId().equals(stopTime.getTrip().getServiceId().getId()))
                date = checkDaysOfTheWeek(c,today,dateToday);
        });

        if(!date){
            calendarDates.forEach(cdate->{
                if(cdate.getServiceId().getId().equals(stopTime.getTrip().getServiceId().getId())){
                    if(cdate.getDate().getAsDate().compareTo(dateToday)==0){
                        date = true;
                    }
                }
            });
        }

        return date;
    }


    public boolean checkDaysOfTheWeek(ServiceCalendar serviceCalendar, LocalDate today, Date date){
        Integer dayoftheweek = today.getDayOfWeek().getValue();
        boolean flag = false;

        if((serviceCalendar.getStartDate().getAsDate().before(date) && serviceCalendar.getEndDate().getAsDate().after(date)) ||
                serviceCalendar.getStartDate().getAsDate().equals(date) || serviceCalendar.getEndDate().getAsDate().equals(date)) {
            switch (dayoftheweek) {
                case 1:
                    if (serviceCalendar.getMonday() == 1) {
                        flag = true;
                    }
                    break;
                case 2:
                    if (serviceCalendar.getTuesday() == 1) {
                        flag = true;
                    }
                    break;
                case 3:
                    if (serviceCalendar.getWednesday() == 1) {
                        flag = true;
                    }
                    break;
                case 4:
                    if (serviceCalendar.getThursday() == 1) {
                        flag = true;
                    }
                    break;
                case 5:
                    if (serviceCalendar.getFriday() == 1) {
                        flag = true;
                    }
                    break;
                case 6:
                    if (serviceCalendar.getSaturday() == 1) {
                        flag = true;
                    }
                    break;
                case 7:
                    if (serviceCalendar.getSunday() == 1) {
                        flag = true;
                    }
                    break;
            }
        }
        return flag;
    }




}


