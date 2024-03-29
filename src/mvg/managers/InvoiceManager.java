package mvg.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import mvg.auxilary.*;
import mvg.model.*;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ghost on 2017/01/27.
 */
public class InvoiceManager extends MVGObjectManager
{
    private HashMap<String, Invoice> invoices;
    private static InvoiceManager invoice_manager = new InvoiceManager();
    private Gson gson;
    public static final String ROOT_PATH = "cache/invoices/";
    public String filename = "";
    private long timestamp;
    private static final String TAG = "InvoiceManager";

    private InvoiceManager()
    {
    }

    public static InvoiceManager getInstance()
    {
        return invoice_manager;
    }

    @Override
    public void initialize()
    {
        synchroniseDataset();
    }

    @Override
    public HashMap<String, Invoice> getDataset()
    {
        return invoices;
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
                            String timestamp_json = RemoteComms.sendGetRequest("/timestamp/invoices_timestamp", headers);
                            Counters cntr_timestamp = gson.fromJson(timestamp_json, Counters.class);
                            if(cntr_timestamp!=null)
                            {
                                timestamp = cntr_timestamp.getCount();
                                filename = "invoices_"+timestamp+".dat";
                                IO.log(this.getClass().getName(), IO.TAG_INFO, "Server Timestamp: "+timestamp);
                            }else {
                                IO.log(this.getClass().getName(), IO.TAG_ERROR, "could not get valid timestamp");
                                return null;
                            }

                            if(!isSerialized(ROOT_PATH+filename))
                            {
                                String invoices_json = RemoteComms.sendGetRequest("/invoices/"+smgr.getActiveUser().getOrganisation_id(), headers);
                                InvoiceServerObject invoiceServerObject= gson.fromJson(invoices_json, InvoiceServerObject.class);
                                if(invoiceServerObject!=null)
                                {
                                    if(invoiceServerObject.get_embedded()!=null)
                                    {
                                        Invoice[] invoices_arr = invoiceServerObject.get_embedded().getInvoices();
                                        invoices = new HashMap<>();
                                        for (Invoice invoice : invoices_arr)
                                            invoices.put(invoice.get_id(), invoice);
                                    } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not find any Invoices in the database.");
                                } else IO.log(getClass().getName(), IO.TAG_ERROR, "InvoiceServerObject (containing Invoice objects & other metadata) is null");

                                IO.log(getClass().getName(), IO.TAG_INFO, "reloaded collection of invoices.");
                                serialize(ROOT_PATH+filename, invoices);
                            } else
                            {
                                IO.log(this.getClass().getName(), IO.TAG_INFO, "binary object ["+ROOT_PATH+filename+"] on local disk is already up-to-date.");
                                invoices = (HashMap<String, Invoice>) deserialize(ROOT_PATH+filename);
                            }
                        } else IO.logAndAlert("Session Expired", "Active session has expired.", IO.TAG_ERROR);
                    } else IO.logAndAlert("No active sessions.", "Active Session is invalid", IO.TAG_ERROR);
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

    public void requestInvoiceApproval(Invoice invoice, Callback callback) throws IOException
    {
        if(invoice==null)
        {
            IO.logAndAlert("Error", "Invalid Invoice.", IO.TAG_ERROR);
            return;
        }
        if(invoice.getClient()==null)
        {
            IO.logAndAlert("Error", "Invalid Invoice Client object.", IO.TAG_ERROR);
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
            IO.logAndAlert("Error: Invalid Employee Session", "Could not find any active user sessions.", IO.TAG_ERROR);
            return;
        }
        /*String path = PDF.createInvoicePdf(invoice);
        String base64_invoice = null;
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
                    base64_invoice = Base64.getEncoder().encodeToString(buffer);
                } else
                {
                    IO.logAndAlert(InvoiceManager.class.getName(), "File [" + path + "] not found.", IO.TAG_ERROR);
                }
            } else
            {
                IO.log(InvoiceManager.class.getName(), "File [" + path + "] object is null.", IO.TAG_ERROR);
            }
        } else IO.log(InvoiceManager.class.getName(), "Could not get valid path for created Invoice PDF.", IO.TAG_ERROR);*/
        final String finalBase64_invoice = "base64_invoice";

        Stage stage = new Stage();
        stage.setTitle(Globals.APP_NAME.getValue() + " - Request Invoice ["+invoice.get_id()+"] Approval");
        stage.setMinWidth(320);
        stage.setHeight(350);
        stage.setAlwaysOnTop(true);

        VBox vbox = new VBox(1);

        final TextField txt_subject = new TextField();
        txt_subject.setMinWidth(200);
        txt_subject.setMaxWidth(Double.MAX_VALUE);
        txt_subject.setPromptText("Type in an eMail subject");
        txt_subject.setText("INVOICE ["+invoice.get_id()+"] APPROVAL REQUEST");
        HBox subject = CustomTableViewControls.getLabelledNode("Subject: ", 200, txt_subject);

        final TextArea txt_message = new TextArea();
        txt_message.setMinWidth(200);
        txt_message.setMaxWidth(Double.MAX_VALUE);
        HBox message = CustomTableViewControls.getLabelledNode("Message: ", 200, txt_message);

        //set default message
        User sender = SessionManager.getInstance().getActiveUser();
        String title = sender.getGender().toLowerCase().equals("male") ? "Mr." : "Miss.";;
        String def_msg = "Good day,\n\nCould you please assist me" +
                " by approving this invoice to be issued to "  + invoice.getClient().getClient_name() + ".\nThank you.\n\nBest Regards,\n"
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
                headers.add(new AbstractMap.SimpleEntry<>("invoice_id", invoice.get_id()));
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
                FileMetadata fileMetadata = new FileMetadata("invoice_"+invoice.get_id()+".pdf","application/pdf");
                fileMetadata.setCreator(SessionManager.getInstance().getActive().getUsername());
                fileMetadata.setFile(finalBase64_invoice);
                HttpURLConnection connection = RemoteComms.postJSON("/invoices/approval_request", fileMetadata.asJSONString(), headers);
                if(connection!=null)
                {
                    if(connection.getResponseCode()==HttpURLConnection.HTTP_OK)
                    {
                        //TODO: CC self
                        IO.logAndAlert("Success", "Successfully requested Invoice approval!", IO.TAG_INFO);

                        //dismiss stage if successful
                        Platform.runLater(() ->
                        {
                            if(stage!=null)
                                if(stage.isShowing())
                                    stage.close();
                        });

                        //execute callback w/ args
                        if(callback!=null)
                            callback.call(IO.readStream(connection.getInputStream()));
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

        //Add form controls vertically on the stage
        //vbox.getChildren().add(destination);
        vbox.getChildren().add(subject);
        //vbox.getChildren().add(hbox_quote_id);
        vbox.getChildren().add(message);
        vbox.getChildren().add(submit);

        //Setup scene and display stage
        Scene scene = new Scene(vbox);
        File fCss = new File(IO.STYLES_ROOT_PATH+"home.css");
        scene.getStylesheets().clear();
        scene.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));

        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
        stage.setResizable(true);
    }

    /**
     * Method to create Invoice in the database server
     * @param trip Trip object associated with Invoice.
     * @param quote_revision_numbers Quote revision number to be used to generated the invoice [uses quote_id of Trip class]
     * @param amount_receivable Cash amount received for this Invoice.
     * @param callback Callback to be executed after creation of the Invoice [if successful]
     * @throws IOException
     */
    public void createInvoice(Trip trip, String quote_revision_numbers, double amount_receivable, Callback callback) throws IOException
    {
        if(trip.getQuote()==null)
        {
            IO.logAndAlert(getClass().getName(), "Trip->Quote object is not set.", IO.TAG_ERROR);
            return;
        }
        if(trip.getQuote().getEnquiry()==null)
        {
            IO.logAndAlert(getClass().getName(), "Trip->Quote->Enquiry object is not set.", IO.TAG_ERROR);
            return;
        }
        if(trip.getQuote().getEnquiry().getCreatorUser()==null)
        {
            IO.logAndAlert(getClass().getName(), "Trip->Quote->Enquiry's creator could not be found.", IO.TAG_ERROR);
            return;
        }
        if(quote_revision_numbers==null)
        {
            IO.logAndAlert(getClass().getName(), "Please select valid quote revisions for the new invoice.", IO.TAG_ERROR);
            return;
        }
        if(quote_revision_numbers.isEmpty())
        {
            IO.logAndAlert(getClass().getName(), "Invoice Quote revisions object is empty.", IO.TAG_ERROR);
            return;
        }
        SessionManager smgr = SessionManager.getInstance();
        if(smgr.getActive()!=null)
        {
            if(!smgr.getActive().isExpired())
            {
                gson  = new GsonBuilder().create();
                ArrayList<AbstractMap.SimpleEntry<String,String>> headers = new ArrayList<>();
                headers.add(new AbstractMap.SimpleEntry<>("Cookie", smgr.getActive().getSessionId()));
                headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));

                Invoice invoice = new Invoice();
                invoice.setCreator(smgr.getActiveUser().getUsr());
                invoice.setTrip_id(trip.get_id());
                invoice.setReceivable(amount_receivable);
                invoice.setClient_id(trip.getQuote().getEnquiry().getCreatorUser().getOrganisation_id());

                HttpURLConnection response = RemoteComms.putJSON("/invoices", invoice.asJSONString(), headers);
                if(response!=null)
                {
                    if(response.getResponseCode()==HttpURLConnection.HTTP_OK)
                    {
                        String inv_id = IO.readStream(response.getInputStream());
                        IO.logAndAlert("Success", "Successfully created new Invoice: " + inv_id, IO.TAG_INFO);
                        if(callback!=null)
                            callback.call(inv_id);
                    } else {
                        IO.logAndAlert("Error", IO.readStream(response.getErrorStream()), IO.TAG_ERROR);
                        if(callback!=null)
                            callback.call(null);
                    }
                } else IO.logAndAlert("Error", "Response object is null.", IO.TAG_ERROR);
            } else IO.logAndAlert("Error: Session Expired", "Active session has expired.", IO.TAG_ERROR);
        } else IO.logAndAlert("Error: Invalid Session", "No valid active sessions.", IO.TAG_ERROR);
    }

    class InvoiceServerObject extends ServerObject
    {
        private InvoiceServerObject.Embedded _embedded;

        InvoiceServerObject.Embedded get_embedded()
        {
            return _embedded;
        }

        void set_embedded(InvoiceServerObject.Embedded _embedded)
        {
            this._embedded = _embedded;
        }

        class Embedded
        {
            private Invoice[] invoices;

            public Invoice[] getInvoices()
            {
                return invoices;
            }

            public void setInvoices(Invoice[] invoices)
            {
                this.invoices = invoices;
            }
        }
    }
}
