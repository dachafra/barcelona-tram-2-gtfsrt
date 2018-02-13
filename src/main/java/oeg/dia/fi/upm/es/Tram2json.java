package oeg.dia.fi.upm.es;


import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.json.JSONArray;

public class Tram2json{

    private String key;
    private JSONArray times;

    public Tram2json (){

    }



    public void getKey(){
        this.key="";
        try {
            HttpResponse<JsonNode> jsonResponse = Unirest.post("https://tram-opendata.azurewebsites.net/connect/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("grant_type", "client_credentials")
                    .field("client_id", "------")
                    .field("client_secret", "-------")
                    .asJson();
            this.key = jsonResponse.getBody().getObject().getString("access_token");
        }catch (Exception e){
            System.out.println("Error getting the key: "+e.getMessage());
        }

    }

    public JSONArray recolectTimes(){
        try {
            HttpResponse<JsonNode> jsonResponse = Unirest.get("https://tram-opendata.azurewebsites.net/api/v1/stopTimes")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Bearer "+key)
                    .asJson();
            times = jsonResponse.getBody().getArray();

        }catch (Exception e){
            System.out.println("Error getting the realtime: "+e.getMessage());
            times= new JSONArray();
        }
        return times;
    }
}
