import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

import org.json.JSONArray;
import org.json.JSONObject;


public class App {

    private final static String SQUARE_ACCESS_TOKEN = "lol-no";
    private final static String SQUARE_LOCATION_ID = "YBBP68F1DR33M";

    private final static String SQUARE_URL_ORDER_SEARCH = "https://connect.squareup.com/v2/orders/search"; // POST
    // private final static String SQUARE_URL_ORDER_DETAIL = "https://connect.squareup.com/v2/orders/"; // GET + ID
    
    public static void main(String[] args) throws Exception {
        HttpClient apiClient = HttpClient.newBuilder().build();

        String orderSearchBody = "{\"location_ids\": [\"" + SQUARE_LOCATION_ID + "\"], \"return_entries\": false, \"query\": {" +
            "\"filter\": {\"state_filter\": {\"states\": [\"COMPLETED\"]}, " +
            "\"date_time_filter\": {\"created_at\": {\"start_at\": \"2024-04-01T00:00:00-04:00\", \"end_at\": \"2024-05-01T00:00:00-04:00\"}}}}}";
        HttpRequest orderSearchRequest = HttpRequest.newBuilder(URI.create(SQUARE_URL_ORDER_SEARCH))
            .header("Authorization", "Bearer " + SQUARE_ACCESS_TOKEN)
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(orderSearchBody)
        ).build();

        HttpResponse<String> apiResponse = apiClient.send(orderSearchRequest, BodyHandlers.ofString());
        if (apiResponse.statusCode() == HttpURLConnection.HTTP_OK) {
            JSONObject resp = new JSONObject(apiResponse.body());
            JSONArray orders = resp.getJSONArray("orders");
            for (int i = 0; i < orders.length(); i++) {
                // When tenders[0].type == "OTHER", there will be a tenders[0].note containing the manually-entered text (usually "Venmo")
                // Amount paid, in cents, is in tenders[0].amount_money.amount
                // Is net_amounts.tax_money.amount > 0? - iterate through line items looking for total_tax_money.amount>0, accumulating gross_sales_money.amount to determine a "taxable sales" figure
                // net_amounts.service_charge_money.amount
                // TODO: Where will shipping charges appear for online orders? Need to look back at late 2023 for an example.
                JSONObject order = orders.getJSONObject(i);
                String tenderType = order.getJSONArray("tenders").getJSONObject(0).getString("type");
                if (tenderType.equalsIgnoreCase("other")) {
                    tenderType += " (" + order.getJSONArray("tenders").getJSONObject(0).getString("note") + ")";
                }
                long tenderAmount = order.getJSONArray("tenders").getJSONObject(0).getJSONObject("amount_money").getLong("amount");
                System.out.println("Type: " + tenderType + ", Amount: " + ((double)tenderAmount)/100);
            }
        } else {
            System.out.println("Failed with response code: " + apiResponse.statusCode() + ", Body: " + apiResponse.body());
        }
    }
}
