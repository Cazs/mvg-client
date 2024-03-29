package mvg.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import mvg.auxilary.*;
import mvg.model.*;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.*;

/**
 * Created by ghost on 2017/01/21.
 */
public class QuoteManager extends MVGObjectManager
{
    private HashMap<String, Quote> quotes;
    private Gson gson;
    private static QuoteManager quote_manager = new QuoteManager();
    private long timestamp;
    public static final String ROOT_PATH = "cache/quotes/";
    public String filename = "";
    public int selected_quote_sibling_cursor = 0;
    public static final double VAT = 14.0;

    private QuoteManager()
    {
    }

    @Override
    public void initialize()
    {
        synchroniseDataset();
    }

    public static QuoteManager getInstance()
    {
        return quote_manager;
    }

    @Override
    public HashMap<String, Quote> getDataset()
    {
        return quotes;
    }

    @Override
    Callback getSynchronisationCallback()
    {
        return new Callback()
        {
            @Override
            public Object call(Object param)
            {
                try
                {
                    SessionManager smgr = SessionManager.getInstance();
                    if(smgr.getActive()!=null)
                    {
                        if(!smgr.getActive().isExpired())
                        {

                            gson  = new GsonBuilder().create();
                            ArrayList<AbstractMap.SimpleEntry<String,String>> headers = new ArrayList<>();
                            headers.add(new AbstractMap.SimpleEntry<>("Cookie", smgr.getActive().getSessionId()));
                            //Get Timestamp
                            String quotes_timestamp_json = RemoteComms.sendGetRequest("/timestamp/quotes_timestamp", headers);
                            Counters quotes_timestamp = gson.fromJson(quotes_timestamp_json, Counters.class);
                            if(quotes_timestamp!=null)
                            {
                                timestamp = quotes_timestamp.getCount();
                                filename = "quotes_"+timestamp+".dat";
                                IO.log(QuoteManager.getInstance().getClass().getName(), IO.TAG_INFO, "Server Timestamp: "+quotes_timestamp.getCount());
                            } else {
                                IO.logAndAlert(this.getClass().getName(), "could not get valid timestamp", IO.TAG_ERROR);
                                return null;
                            }

                            if(!isSerialized(ROOT_PATH+filename))
                            {
                                //Load Quotes
                                String quotes_json = RemoteComms.sendGetRequest("/quotes/"+smgr.getActiveUser().getOrganisation_id(), headers);
                                QuoteServerObject quoteServerObject = gson.fromJson(quotes_json, QuoteServerObject.class);
                                if(quoteServerObject!=null)
                                {
                                    if(quoteServerObject.get_embedded()!=null)
                                    {
                                        Quote[] quotes_arr = quoteServerObject.get_embedded().getQuotes();
                                        quotes = new HashMap<>();
                                        for (Quote quote : quotes_arr)
                                            quotes.put(quote.get_id(), quote);
                                    } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not find any Quotes in database.");
                                } else IO.log(getClass().getName(), IO.TAG_ERROR, "QuoteServerObject (containing Quote objects & other metadata) is null");

                                User[] users = new User[UserManager.getInstance().getDataset().values().toArray().length];
                                UserManager.getInstance().getDataset().values().toArray(users);

                                if(quotes!=null)
                                {
                                    if (!quotes.isEmpty())
                                    {
                                        for (Quote quote : quotes.values())
                                        {
                                            //Load Quote Resources
                                            String quote_item_ids_json = RemoteComms
                                                    .sendGetRequest("/quotes/resources/" + quote.get_id(), headers);
                                            if (quote_item_ids_json != null)
                                            {
                                                if (!quote_item_ids_json.equals("[]"))
                                                {
                                                    QuoteResourceServerObject quoteResourceServerObject = gson
                                                            .fromJson(quote_item_ids_json, QuoteResourceServerObject.class);
                                                    if (quoteResourceServerObject != null)
                                                    {
                                                        if (quoteResourceServerObject.get_embedded() != null)
                                                        {
                                                            QuoteItem[] quote_resources_arr = quoteResourceServerObject
                                                                    .get_embedded()
                                                                    .getQuote_resources();
                                                            quote.setResources(quote_resources_arr);
                                                            IO.log(getClass().getName(), IO.TAG_INFO, String
                                                                    .format("set resources for quote '%s'.", quote.get_id()));
                                                        }
                                                        else IO.log(getClass()
                                                                .getName(), IO.TAG_ERROR, "could not find any Resources for Quote #" + quote
                                                                .get_id());
                                                    }
                                                    else IO.log(getClass()
                                                            .getName(), IO.TAG_ERROR, "QuoteResourceServerObject (containing QuoteItem objects & other metadata) is null");
                                                }
                                                else IO.log(getClass().getName(), IO.TAG_WARN, String
                                                        .format("quote '%s does not have any resources.", quote.get_id()));
                                            }
                                            else IO.log(getClass().getName(), IO.TAG_WARN, String
                                                    .format("quote '%s does not have any resources.", quote.get_id()));
                                        }
                                        IO.log(getClass().getName(), IO.TAG_INFO, "reloaded collection of quotes.");
                                        serialize(ROOT_PATH + filename, quotes);
                                    }
                                    else
                                    {
                                        IO.log(getClass().getName(), IO.TAG_ERROR, "no quotes found in database.");
                                    }
                                } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not find any Quotes in database.");
                            } else {
                                IO.log(this.getClass().getName(), IO.TAG_INFO, "binary object ["+ROOT_PATH+filename+"] on local disk is already up-to-date.");
                                quotes = (HashMap<String, Quote>) deserialize(ROOT_PATH+filename);
                            }
                        } else IO.logAndAlert("Session Expired", "Active session has expired.", IO.TAG_ERROR);
                    } else IO.logAndAlert("Invalid Session", "No valid active sessions were found.", IO.TAG_ERROR);
                } catch (ClassNotFoundException e)
                {
                    IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                } catch (IOException e)
                {
                    IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                }
                return null;
            }
        };
    }

    public static double computeQuoteTotal(List<QuoteItem> quoteItems)
    {
        //compute total
        double total=0;
        for(QuoteItem item:  quoteItems)
            total += item.getTotal();
        return total;
    }

    public void createQuote(Quote quote, ObservableList<QuoteItem> quoteItems, Callback callback) throws IOException
    {
        ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
        headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
        if(SessionManager.getInstance().getActive()!=null)
            headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));
        else
        {
            IO.logAndAlert("Session Expired", "No active sessions.", IO.TAG_ERROR);
            return;
        }

        //create new quote on database
        //ArrayList<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();
        HttpURLConnection connection = RemoteComms.putJSON("/quotes", quote.asJSONString(), headers);
        if(connection!=null)
        {
            if(connection.getResponseCode()==HttpURLConnection.HTTP_OK)
            {
                String response = IO.readStream(connection.getInputStream());

                if(response==null)
                {
                    IO.logAndAlert("New Quote Creation Error", "Invalid server response.", IO.TAG_ERROR);
                    return;
                }
                if(response.isEmpty())
                {
                    IO.logAndAlert("New Quote Creation Error", "Invalid server response.", IO.TAG_ERROR);
                    return;
                }

                //server will return message object in format "<quote_id>"
                String new_quote_id = response.replaceAll("\"","");//strip inverted commas around quote_id
                new_quote_id = new_quote_id.replaceAll("\n","");//strip new line chars
                new_quote_id = new_quote_id.replaceAll(" ","");//strip whitespace chars
                IO.log(getClass().getName(), IO.TAG_INFO, "created Quote["+new_quote_id+"]. Adding representatives and Resources to Quote.");

                //Close connection
                if(connection!=null)
                    connection.disconnect();
                /* Add Quote Resources to Quote on database */

                boolean added_all_quote_items = true;
                if(quoteItems!=null)
                {
                    for (QuoteItem quoteItem : quoteItems)
                    {
                        quoteItem.setQuote_id(new_quote_id);
                        added_all_quote_items = QuoteManager.getInstance().createQuoteItem(quoteItem, headers);
                    }
                } else IO.log(getClass().getName(), IO.TAG_WARN, "Quote["+new_quote_id+"] has no items/resources.");
                if(added_all_quote_items)
                {
                    //refresh dataset
                    forceSynchronise();

                    //set selected quote
                    if(QuoteManager.getInstance().getDataset()!=null)
                        QuoteManager.getInstance().setSelected(QuoteManager.getInstance().getDataset().get(new_quote_id));

                    IO.logAndAlert("New Quote Creation Success", "Successfully created a new Quote: "+new_quote_id, IO.TAG_INFO);

                    //execute callback w/ args
                    if(callback!=null)
                        if(new_quote_id!=null)
                        {
                            callback.call(new_quote_id);
                        } else callback.call(true);
                } else
                {
                    IO.logAndAlert("New Quote Creation Failure", "Could not add items to quote.", IO.TAG_ERROR);

                    //execute callback w/o args
                    if(callback!=null)
                        callback.call(null);
                }
            } else
            {
                //Get error message
                String msg = IO.readStream(connection.getErrorStream());
                IO.logAndAlert("Error " +String.valueOf(connection.getResponseCode()), msg, IO.TAG_ERROR);
            }
            if(connection!=null)
                connection.disconnect();
        } else IO.logAndAlert("New Quote Creation Failure", "Could not connect to server.", IO.TAG_ERROR);
    }

    public void updateQuote(Quote quote, ObservableList<QuoteItem> quoteItems)
    {
        if(quoteItems==null)
        {
            IO.logAndAlert("Invalid Quote", "Quote items list is null.", IO.TAG_ERROR);
            return;
        }
        if(quoteItems.size()<=0)
        {
            IO.logAndAlert("Invalid Quote", "Quote has no items", IO.TAG_ERROR);
            return;
        }
        QuoteItem[] items = new QuoteItem[quoteItems.size()];
        quoteItems.toArray(items);

        updateQuote(quote, items);
    }

    public void updateQuote(Quote quote, QuoteItem[] quoteItems)
    {
        if (SessionManager.getInstance().getActive() == null)
        {
            IO.logAndAlert("Session Expired", "No active sessions.", IO.TAG_ERROR);
            return;
        }
        if (SessionManager.getInstance().getActive().isExpired())
        {
            IO.logAndAlert("Session Expired", "Active session has expired.", IO.TAG_ERROR);
            return;
        }
        if(quoteItems==null)
        {
            IO.logAndAlert("Invalid Quote", "Quote items list is null.", IO.TAG_ERROR);
            return;
        }
        if(quoteItems.length<=0)
        {
            IO.logAndAlert("Invalid Quote", "Quote has no items", IO.TAG_ERROR);
            return;
        }

        //Quote selected = getSelectedQuote();
        if(quote!=null)
        {
            //prepare quote parameters
            try
            {
                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
                headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));
                //update quote on database
                HttpURLConnection connection = RemoteComms.postJSON("/quotes", quote.asJSONString(), headers);
                if (connection != null)
                {
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                    {
                        String response = IO.readStream(connection.getInputStream());
                        IO.log(getClass().getName(), IO.TAG_INFO, "updated quote[" + quote.get_id() + "]. adding resources to quote.");

                        if (response == null)
                        {
                            IO.logAndAlert("Quote Update Error", "Invalid server response.", IO.TAG_ERROR);
                            return;
                        }
                        if (response.isEmpty())
                        {
                            IO.logAndAlert("Quote Update Error", "Invalid server response: " + response, IO.TAG_ERROR);
                            return;
                        }

                        boolean updated_all_quote_items = updateQuoteItems(quote.get_id(), quoteItems, headers);

                        if (updated_all_quote_items)
                        {
                            IO.logAndAlert("Quote Manager","Successfully updated quote[" + quote.get_id() + "].", IO.TAG_INFO);
                            forceSynchronise();
                        } else
                        {
                            IO.logAndAlert("Quote Update Failure", "Could not update all Quote Items for Quote["+quote.get_id()+"].", IO.TAG_ERROR);
                        }
                    } else
                    {
                        //Get error message
                        String msg = IO.readStream(connection.getErrorStream());
                        IO.logAndAlert("Error " + String.valueOf(connection.getResponseCode()), msg, IO.TAG_ERROR);
                    }

                    //Close connection
                    if (connection != null)
                        connection.disconnect();
                } else IO.logAndAlert("Quote Update Failure", "Could not connect to server.", IO.TAG_ERROR);
            } catch (IOException e)
            {
                IO.logAndAlert(getClass().getName(), e.getMessage(), IO.TAG_ERROR);
            }
        } else IO.logAndAlert("Error","Selected Quote is invalid.", IO.TAG_ERROR);
    }

    public boolean updateQuoteItems(String quote_id, QuoteItem[] quoteItems, ArrayList headers) throws IOException
    {
        if(quote_id==null || quoteItems==null || headers == null)
            return false;

        boolean all_successful = true;
        /* Update/Create QuoteItems on database */
        for (QuoteItem quoteItem : quoteItems)
        {
            if (quoteItem != null)
            {
                /*
                    if QuoteItem has an ID then it's been already
                    added to the database - then update it, else create new record on db.
                 */
                if (quoteItem.get_id() != null)
                {
                    //set quote_item creator
                    quoteItem.setCreator(SessionManager.getInstance().getActive().getUsername());
                    //update quote_item
                    all_successful = updateQuoteItem(quoteItem, headers);
                } else
                {
                    //new quote_item
                    //prepare parameters for quote_item.
                    /*ArrayList params = new ArrayList<>();
                    params.add(new AbstractMap.SimpleEntry<>("item_number", quoteItem.getItem_number()));
                    params.add(new AbstractMap.SimpleEntry<>("resource_id", quoteItem.getResource().get_id()));
                    params.add(new AbstractMap.SimpleEntry<>("quote_id", quote_id));
                    params.add(new AbstractMap.SimpleEntry<>("markup", quoteItem.getMarkup()));
                    params.add(new AbstractMap.SimpleEntry<>("labour", quoteItem.getLabour()));
                    params.add(new AbstractMap.SimpleEntry<>("quantity", quoteItem.getQuantity()));
                    params.add(new AbstractMap.SimpleEntry<>("additional_costs", quoteItem.getAdditional_costs()));*/


                    quoteItem.setQuote_id(quote_id);
                    quoteItem.setResource_id(quoteItem.getResource().get_id());

                    all_successful = createQuoteItem(quoteItem, headers);
                }
            } else IO.log(getClass().getName(), IO.TAG_ERROR, "invalid[null] quote_item.");
        }
        return all_successful;
    }

    public boolean createQuoteItem(QuoteItem quoteItem, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        if(quoteItem==null)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, "invalid quote_item.");
            return false;
        }
        if(SessionManager.getInstance().getActive()==null)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, "invalid active User Session.");
            return false;
        }
        //set quote_item creator
        quoteItem.setCreator(SessionManager.getInstance().getActive().getUsername());
        //IO.log(getClass().getName(), IO.TAG_INFO, "attempting to create new quote_item for quote[" + quote_id + "].");
        HttpURLConnection connection = RemoteComms.putJSON("/quotes/resources", quoteItem.asJSONString(), headers);

        if (connection != null)
        {
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                IO.log(getClass().getName(), IO.TAG_INFO, "successfully added a new quote_item.");
                //loadDataFromServer();//refresh data set
                //Close connection
                if (connection != null)
                    connection.disconnect();
                return true;
            } else
            {
                //Get error message
                String msg = IO.readStream(connection.getErrorStream());
                IO.logAndAlert("Error " + String.valueOf(connection.getResponseCode()), msg, IO.TAG_ERROR);
            }
        } else IO.logAndAlert("New Quote Item Creation Failure", "Could not connect to server.", IO.TAG_ERROR);
        //Close connection
        if (connection != null)
            connection.disconnect();
        return false;
    }

    public boolean createQuoteItem(ArrayList params,ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        HttpURLConnection connection = RemoteComms.putJSONData("/quotes/resources", params, headers);
        if (connection != null)
        {
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                IO.log(getClass().getName(), IO.TAG_INFO, "successfully added a new quote_resource.");
                //Close connection
                if (connection != null)
                    connection.disconnect();
                return true;
            } else
            {
                //Get error message
                String msg = IO.readStream(connection.getErrorStream());
                IO.logAndAlert("Error " + String.valueOf(connection.getResponseCode()), msg, IO.TAG_ERROR);
            }
        } else IO.logAndAlert("New Quote Resource Creation Failure", "Could not connect to server.", IO.TAG_ERROR);
        //Close connection
        if (connection != null)
            connection.disconnect();
        return false;
    }

    public boolean updateQuoteItem(QuoteItem quoteItem, ArrayList<AbstractMap.SimpleEntry<String,String>> headers) throws IOException
    {
        if(quoteItem!=null)
        {
            IO.log(getClass().getName(), IO.TAG_INFO, "attempting to update quote_item["+quoteItem.get_id()+"] for quote[" + quoteItem.getQuote_id() + "].");
            HttpURLConnection connection = RemoteComms.postJSON("/quotes/resources", quoteItem.asJSONString(), headers);
            if (connection != null)
            {
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    IO.log(getClass().getName(), IO.TAG_INFO, "successfully updated quote_item[" + quoteItem.get_id() + "] for quote[" + quoteItem.getQuote_id() + "].");
                    //Close connection
                    if (connection != null)
                        connection.disconnect();
                    return true;
                } else
                {
                    String msg = IO.readStream(connection.getErrorStream());
                    IO.log(getClass().getName(),IO.TAG_ERROR,"Error " + String.valueOf(connection.getResponseCode()) + ":" + msg);
                }
            } else IO.logAndAlert("Quote Item Update Failure", "Could not connect to server.", IO.TAG_ERROR);
            //Close connection
            if (connection != null)
                connection.disconnect();
        }else IO.log(getClass().getName(), IO.TAG_ERROR, "invalid[null] quote_item.");
        return false;
    }

    public static void emailQuote(Quote quote, Callback callback)
    {
        if(quote==null)
        {
            IO.logAndAlert("Error", "Invalid Quote.", IO.TAG_ERROR);
            return;
        }

        //upload Quote PDF to server
        uploadQuotePDF(quote);

        Stage stage = new Stage();
        stage.setTitle(Globals.APP_NAME.getValue() + " - eMail Quote ["+quote.get_id()+"]");
        stage.setMinWidth(320);
        stage.setHeight(350);
        stage.setAlwaysOnTop(true);

        VBox vbox = new VBox(1);

        final TextField txt_destination = new TextField();
        txt_destination.setMinWidth(200);
        txt_destination.setMaxWidth(Double.MAX_VALUE);
        txt_destination.setPromptText("Type in email address/es separated by commas");
        HBox destination = CustomTableViewControls.getLabelledNode("To: ", 200, txt_destination);

        final TextField txt_subject = new TextField();
        txt_subject.setMinWidth(200);
        txt_subject.setMaxWidth(Double.MAX_VALUE);
        txt_subject.setPromptText("Type in an eMail subject");
        HBox subject = CustomTableViewControls.getLabelledNode("Subject: ", 200, txt_subject);

        final TextField txt_quote_id = new TextField();
        txt_quote_id.setMinWidth(200);
        txt_quote_id.setMaxWidth(Double.MAX_VALUE);
        txt_quote_id.setPromptText("Type in a message");
        txt_quote_id.setEditable(false);
        txt_quote_id.setText(String.valueOf(quote.get_id()));
        HBox hbox_quote_id = CustomTableViewControls.getLabelledNode("Quote ID: ", 200, txt_quote_id);

        final TextArea txt_message = new TextArea();
        txt_message.setMinWidth(200);
        txt_message.setMaxWidth(Double.MAX_VALUE);
        HBox message = CustomTableViewControls.getLabelledNode("Message: ", 200, txt_message);

        HBox submit;
        submit = CustomTableViewControls.getSpacedButton("Send", event ->
        {
            String date_regex="\\d+(\\-|\\/|\\\\)\\d+(\\-|\\/|\\\\)\\d+";

            if(!Validators.isValidNode(txt_destination, txt_destination.getText(), 1, ".+"))
                return;
            if(!Validators.isValidNode(txt_subject, txt_subject.getText(), 1, ".+"))
                return;
            if(!Validators.isValidNode(txt_message, txt_message.getText(), 1, ".+"))
                return;

            String str_destination = txt_destination.getText();
            String str_subject = txt_subject.getText();
            String str_message = txt_message.getText();

            ArrayList<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();
            params.add(new AbstractMap.SimpleEntry<>("quote_id", quote.get_id()));
            params.add(new AbstractMap.SimpleEntry<>("to_email", str_destination));
            params.add(new AbstractMap.SimpleEntry<>("subject", str_subject));
            params.add(new AbstractMap.SimpleEntry<>("message", str_message));
            try
            {
                //send email
                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                if(SessionManager.getInstance().getActive()!=null)
                {
                    headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));
                    params.add(new AbstractMap.SimpleEntry<>("from_name", SessionManager.getInstance().getActiveUser().getName()));
                } else
                {
                    IO.logAndAlert( "No active sessions.", "Session expired", IO.TAG_ERROR);
                    return;
                }

                HttpURLConnection connection = RemoteComms.postData("/quotes/mailto", params, headers);
                if(connection!=null)
                {
                    if(connection.getResponseCode()==HttpURLConnection.HTTP_OK)
                    {
                        IO.logAndAlert("Success", "Successfully emailed quote to ["+txt_destination.getText()+"]!", IO.TAG_INFO);

                        //dismiss stage if successful
                        Platform.runLater(() ->
                        {
                            if(stage!=null)
                                if(stage.isShowing())
                                    stage.close();
                        });

                        //execute callback w/ args
                        if(callback!=null)
                            callback.call(true);
                    } else
                    {
                        IO.logAndAlert( "ERROR_" + connection.getResponseCode(),  IO.readStream(connection.getErrorStream()), IO.TAG_ERROR);
                        //execute callback w/ args
                        if(callback!=null)
                            callback.call(null);
                    }
                    connection.disconnect();
                }
            } catch (IOException e)
            {
                IO.log(QuoteManager.class.getName(), IO.TAG_ERROR, e.getMessage());
            }
        });

        //Add form controls vertically on the stage
        vbox.getChildren().add(destination);
        vbox.getChildren().add(subject);
        vbox.getChildren().add(hbox_quote_id);
        vbox.getChildren().add(message);
        vbox.getChildren().add(submit);

        //Setup scene and display stage
        Scene scene = new Scene(vbox);
        File fCss = new File("src/mvg/styles/home.css");
        scene.getStylesheets().clear();
        scene.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));

        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
        stage.setResizable(true);
    }

    public void requestQuoteApproval(Quote quote, Callback callback) throws IOException
    {
        if(quote==null)
        {
            IO.logAndAlert("Error", "Invalid Quote.", IO.TAG_ERROR);
            return;
        }
        if(UserManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert("Error", "Could not find any users in the system.", IO.TAG_ERROR);
            return;
        }
        if(SessionManager.getInstance().getActive()==null)
        {
            IO.logAndAlert("Error: Invalid Session", "Could not find any valid sessions.", IO.TAG_ERROR);
            return;
        }
        if(SessionManager.getInstance().getActive().isExpired())
        {
            IO.logAndAlert("Error: Session Expired", "The active session has expired.", IO.TAG_ERROR);
            return;
        }
        if(SessionManager.getInstance().getActiveUser()==null)
        {
            IO.logAndAlert("Error: Invalid User Session", "Could not find any active user sessions.", IO.TAG_ERROR);
            return;
        }
        String path = PDF.createQuotePdf(quote);
        String base64_quote = null;
        if(path!=null)
        {
            File f = new File(path);
            if (f != null)
            {
                if (f.exists())
                {
                    FileInputStream in = new FileInputStream(f);
                    byte[] buffer =new byte[(int) f.length()];
                    in.read(buffer, 0, buffer.length);
                    in.close();
                    base64_quote = Base64.getEncoder().encodeToString(buffer);
                    //RemoteComms.uploadFile("/quotes/request_", );
                } else
                {
                    IO.logAndAlert(QuoteManager.class.getName(), "File [" + path + "] not found.", IO.TAG_ERROR);
                }
            } else
            {
                IO.log(QuoteManager.class.getName(), "File [" + path + "] object is null.", IO.TAG_ERROR);
            }
        } else IO.log(QuoteManager.class.getName(), "Could not get valid path for created Quote pdf.", IO.TAG_ERROR);
        final String finalBase64_quote = base64_quote;
        //upload Quote PDF to server
        //uploadQuotePDF(quote);

        Stage stage = new Stage();
        stage.setTitle(Globals.APP_NAME.getValue() + " - Request Quote ["+quote.get_id()+"] Approval");
        stage.setMinWidth(320);
        stage.setHeight(350);
        stage.setAlwaysOnTop(true);

        VBox vbox = new VBox(1);

        final TextField txt_subject = new TextField();
        txt_subject.setMinWidth(200);
        txt_subject.setMaxWidth(Double.MAX_VALUE);
        txt_subject.setPromptText("Type in an eMail subject");
        txt_subject.setText("QUOTE ["+quote.get_id()+"] APPROVAL REQUEST");
        HBox subject = CustomTableViewControls.getLabelledNode("Subject: ", 200, txt_subject);

        final TextArea txt_message = new TextArea();
        txt_message.setMinWidth(200);
        txt_message.setMaxWidth(Double.MAX_VALUE);
        HBox message = CustomTableViewControls.getLabelledNode("Message: ", 200, txt_message);

        //set default message
        User sender = SessionManager.getInstance().getActiveUser();
        String title = sender.getGender().toLowerCase().equals("male") ? "Mr." : "Miss.";;
        String def_msg = "Good day,\n\nCould you please assist me" +
                " by approving this quote to be issued to "  + quote.getClient().getClient_name() + ".\nThank you.\n\nBest Regards,\n"
                + title + " " + sender.getFirstname().toCharArray()[0]+". "+sender.getLastname();
        txt_message.setText(def_msg);

        HBox submit;
        submit = CustomTableViewControls.getSpacedButton("Send", event ->
        {
            String date_regex="\\d+(\\-|\\/|\\\\)\\d+(\\-|\\/|\\\\)\\d+";

            //TODO: check this
            //if(!Validators.isValidNode(cbx_destination, cbx_destination.getValue()==null?"":cbx_destination.getValue().getEmail(), 1, ".+"))
            //    return;
            if(!Validators.isValidNode(txt_subject, txt_subject.getText(), 1, ".+"))
                return;
            if(!Validators.isValidNode(txt_message, txt_message.getText(), 1, ".+"))
                return;

            String msg = txt_message.getText();

            //convert all new line chars to HTML break-lines
            msg = msg.replaceAll("\\n", "<br/>");

            //ArrayList<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();
            //params.add(new AbstractMap.SimpleEntry<>("message", msg));

            try
            {
                //send email
                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));//multipart/form-data
                headers.add(new AbstractMap.SimpleEntry<>("quote_id", quote.get_id()));
                //headers.add(new AbstractMap.SimpleEntry<>("to_email", cbx_destination.getValue().getEmail()));
                headers.add(new AbstractMap.SimpleEntry<>("message", msg));
                headers.add(new AbstractMap.SimpleEntry<>("subject", txt_subject.getText()));

                if(SessionManager.getInstance().getActive()!=null)
                {
                    headers.add(new AbstractMap.SimpleEntry<>("session_id", SessionManager.getInstance().getActive().getSessionId()));
                    headers.add(new AbstractMap.SimpleEntry<>("from_name", SessionManager.getInstance().getActiveUser().getName()));
                } else
                {
                    IO.logAndAlert( "No active sessions.", "Session expired", IO.TAG_ERROR);
                    return;
                }

                //String data = "{\"file\":\""+finalBase64_quote+"\"}";
                FileMetadata fileMetadata = new FileMetadata("quote_"+quote.get_id()+".pdf","application/pdf");
                fileMetadata.setFile(finalBase64_quote);
                HttpURLConnection connection = RemoteComms.postJSON("/quotes/approval_request", fileMetadata.asJSONString(), headers);
                if(connection!=null)
                {
                    if(connection.getResponseCode()==HttpURLConnection.HTTP_OK)
                    {
                        //TODO: CC self
                        IO.logAndAlert("Success", "Successfully requested Quote approval!", IO.TAG_INFO);

                        //dismiss stage if successful
                        Platform.runLater(() ->
                        {
                            if(stage!=null)
                                if(stage.isShowing())
                                    stage.close();
                        });

                        //execute callback w/ args
                        if(callback!=null)
                            callback.call(true);
                    } else
                    {
                        IO.logAndAlert( "ERROR " + connection.getResponseCode(),  IO.readStream(connection.getErrorStream()), IO.TAG_ERROR);
                        //execute callback w/o args
                        if(callback!=null)
                            callback.call(null);
                    }
                    connection.disconnect();
                }
            } catch (IOException e)
            {
                IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
            }
        });

        /*cbx_destination.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            if(newValue==null)
            {
                IO.log(getClass().getName(), "invalid destination address.", IO.TAG_ERROR);
                return;
            }
            User sender = SessionManager.getInstance().getActiveUser();
            String title = null;
            if(newValue.getGender()!=null)
                title = newValue.getGender().toLowerCase().equals("male") ? "Mr." : "Miss.";
            String msg = "Good day " + title + " " + newValue.getLastname() + ",\n\nCould you please assist me" +
                            " by approving this quote to be issued to "  + quote.getClient().getClient_name() + ".\nThank you.\n\nBest Regards,\n"
                            + title + " " + sender.getFirstname().toCharArray()[0]+". "+sender.getLastname();
            txt_message.setText(msg);
        });*/

        //Add form controls vertically on the stage
        //vbox.getChildren().add(destination);
        vbox.getChildren().add(subject);
        //vbox.getChildren().add(hbox_quote_id);
        vbox.getChildren().add(message);
        vbox.getChildren().add(submit);

        //Setup scene and display stage
        Scene scene = new Scene(vbox);
        File fCss = new File("src/mvg/styles/home.css");
        scene.getStylesheets().clear();
        scene.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));

        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
        stage.setResizable(true);
    }

    public static void uploadQuotePDF(Quote quote)
    {
        if(quote==null)
        {
            IO.logAndAlert("Error", "Invalid quote object passed.", IO.TAG_ERROR);
            return;
        }
        //Validate session - also done on server-side don't worry ;)
        SessionManager smgr = SessionManager.getInstance();
        if(smgr.getActive()!=null)
        {
            if(!smgr.getActive().isExpired())
            {
                try
                {
                    String path = PDF.createQuotePdf(quote);
                    if(path!=null)
                    {
                        File f = new File(path);
                        if (f != null)
                        {
                            if (f.exists())
                            {
                                FileInputStream in = new FileInputStream(f);
                                byte[] buffer = new byte[(int) f.length()];
                                in.read(buffer, 0, buffer.length);
                                in.close();

                                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                                headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));
                                headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/pdf"));

                                RemoteComms.uploadFile("/api/quote/upload/" + quote.get_id(), headers, buffer);
                                IO.log(QuoteManager.class.getName(), IO.TAG_INFO, "\n uploaded quote [#" + quote
                                        .get_id() + "], file size: [" + buffer.length + "] bytes.");
                            } else
                            {
                                IO.logAndAlert(QuoteManager.class.getName(), "File [" + path + "] not found.", IO.TAG_ERROR);
                            }
                        } else
                        {
                            IO.log(QuoteManager.class.getName(), "File [" + path + "] object is null.", IO.TAG_ERROR);
                        }
                    } else IO.log(QuoteManager.class.getName(), "Could not get valid path for created quote pdf.", IO.TAG_ERROR);
                } catch (IOException e)
                {
                    IO.log(QuoteManager.class.getName(), e.getMessage(), IO.TAG_ERROR);
                }
            } else IO.showMessage("Session Expired", "Active session has expired.", IO.TAG_ERROR);
        } else IO.showMessage("Session Expired", "No active sessions.", IO.TAG_ERROR);
    }

    class QuoteServerObject extends ServerObject
    {
        private QuoteServerObject.Embedded _embedded;

        QuoteServerObject.Embedded get_embedded()
        {
            return _embedded;
        }

        void set_embedded(QuoteServerObject.Embedded _embedded)
        {
            this._embedded = _embedded;
        }

        class Embedded
        {
            private Quote[] quotes;

            public Quote[] getQuotes()
            {
                return quotes;
            }

            public void setQuotes(Quote[] quotes)
            {
                this.quotes = quotes;
            }
        }
    }

    class QuoteResourceServerObject extends ServerObject
    {
        private Embedded _embedded;

        Embedded get_embedded()
        {
            return _embedded;
        }

        void set_embedded(Embedded _embedded)
        {
            this._embedded = _embedded;
        }

        class Embedded
        {
            private QuoteItem[] quote_resources;

            public QuoteItem[] getQuote_resources()
            {
                return quote_resources;
            }

            public void setQuote_resources(QuoteItem[] quote_resources)
            {
                this.quote_resources = quote_resources;
            }
        }
    }
}
