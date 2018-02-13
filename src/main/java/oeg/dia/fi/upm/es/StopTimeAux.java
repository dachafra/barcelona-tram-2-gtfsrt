package oeg.dia.fi.upm.es;



public class StopTimeAux {
    private String trip_id;
    private Integer stop_sequence;
    private String stop_id;
    private Long delay;
    private Long departureTime;
    private Long arrivalTime;

    public StopTimeAux(Integer stop_sequence, Long delay) {
        this.stop_sequence = stop_sequence;
        this.delay = delay;
    }

    public StopTimeAux() {
    }

    public Integer getStop_sequence() {
        return stop_sequence;
    }

    public void setStop_sequence(Integer stop_sequence) {
        this.stop_sequence = stop_sequence;
    }

    public String getTrip_id() {
        return trip_id;
    }

    public void setTrip_id(String trip_id) {
        this.trip_id = trip_id;
    }

    public Long getDelay() {
        return delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public String getStop_id() {
        return stop_id;
    }

    public void setStop_id(String stop_id) {
        this.stop_id = stop_id;
    }

    public Long getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Long departureTime) {
        this.departureTime = departureTime;
    }

    public Long getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }


}
