package oeg.dia.fi.upm.es;


import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tram2json{


    private static final Logger _log = LoggerFactory.getLogger(Gtfs2java.class);

    public static String getKey(){
        String key="";
        try {
            HttpResponse<JsonNode> jsonResponse = Unirest.post("https://tram-opendata.azurewebsites.net/connect/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("grant_type", "client_credentials")
                    .field("client_id", "")
                    .field("client_secret", "")
                    .asJson();
            key = jsonResponse.getBody().getObject().getString("access_token");
        }catch (Exception e){
            _log.error("Error getting the key: "+e.getMessage());
        }
        return key;

    }

    public static JSONArray recolectTimes(String key){
        if(!key.equals("")) {
            JSONArray times = new JSONArray();
            try {
                HttpResponse<JsonNode> jsonResponse = Unirest.get("https://tram-opendata.azurewebsites.net/api/v1/stopTimes")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Authorization", "Bearer " + key)
                        .asJson();
                if (jsonResponse.getBody().isArray())
                    times = jsonResponse.getBody().getArray();
                else {
                    _log.error("Error: " + jsonResponse.getStatus());
                }

            } catch (Exception e) {
                _log.error("Error getting the realtime: " + e.getMessage());
            }
            return times;
        }
        else{
            _log.error("Key is empty");
            return new JSONArray();
        }
    }
}
