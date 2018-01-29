package mvg.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import mvg.auxilary.IO;
import mvg.managers.*;
import mvg.managers.EnquiryManager;
import mvg.model.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class EnquiriesController extends ScreenController implements Initializable
{
    @FXML
    private TableView<Enquiry> tblEnquiries;
    @FXML
    private TableColumn colId, colClient, colEnquiry, colAddress,
            colDestination,colTripType,colDate,colOther,colAction;
    
    @Override
    public void refreshView()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading enquiries view..");
        if( EnquiryManager.getInstance().getEnquiries()==null)
        {
            IO.logAndAlert(getClass().getName(), "no enquiries were found in the database.", IO.TAG_ERROR);
            return;
        }

        colId.setMinWidth(100);
        colId.setCellValueFactory(new PropertyValueFactory<>("_id"));
        CustomTableViewControls.makeEditableTableColumn(colEnquiry, TextFieldTableCell.forTableColumn(), 80, "enquiry", "/enquiries");
        CustomTableViewControls.makeEditableTableColumn(colAddress, TextFieldTableCell.forTableColumn(), 120, "pickup_location", "/enquiries");
        CustomTableViewControls.makeEditableTableColumn(colDestination, TextFieldTableCell.forTableColumn(), 120, "destination", "/enquiries");
        CustomTableViewControls.makeEditableTableColumn(colTripType, TextFieldTableCell.forTableColumn(), 80, "trip_type", "/enquiries");
        CustomTableViewControls.makeLabelledDatePickerTableColumn(colDate, "date_scheduled");
        CustomTableViewControls.makeEditableTableColumn(colOther, TextFieldTableCell.forTableColumn(), 50, "other", "/enquiries");

        ObservableList<Enquiry> lst_enquiries = FXCollections.observableArrayList();
        lst_enquiries.addAll(EnquiryManager.getInstance().getEnquiries().values());
        tblEnquiries.setItems(lst_enquiries);

        Callback<TableColumn<Enquiry, String>, TableCell<Enquiry, String>> cellFactory
                =
                new Callback<TableColumn<Enquiry, String>, TableCell<Enquiry, String>>()
                {
                    @Override
                    public TableCell call(final TableColumn<Enquiry, String> param)
                    {
                        final TableCell<Enquiry, String> cell = new TableCell<Enquiry, String>()
                        {
                            final Button btnQuote = new Button("New Quote");
                            final Button btnRemove = new Button("Delete");

                            @Override
                            public void updateItem(String item, boolean empty)
                            {
                                super.updateItem(item, empty);
                                btnQuote.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
                                btnQuote.getStyleClass().add("btnApply");
                                btnQuote.setMinWidth(100);
                                btnQuote.setMinHeight(35);
                                HBox.setHgrow(btnQuote, Priority.ALWAYS);

                                btnRemove.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
                                btnRemove.getStyleClass().add("btnBack");
                                btnRemove.setMinWidth(100);
                                btnRemove.setMinHeight(35);
                                HBox.setHgrow(btnRemove, Priority.ALWAYS);

                                if (empty)
                                {
                                    setGraphic(null);
                                    setText(null);
                                } else
                                {
                                    HBox hBox = new HBox(btnQuote, btnRemove);
                                    Enquiry enquiry = getTableView().getItems().get(getIndex());

                                    btnQuote.setOnAction(event ->
                                    {
                                        if(enquiry==null)
                                        {
                                            IO.logAndAlert("Error " + getClass().getName(), "Requisition object is not set", IO.TAG_ERROR);
                                            return;
                                        }
                                        if(SessionManager.getInstance().getActive()==null)
                                        {
                                            IO.logAndAlert("Error " + getClass().getName(), "Invalid active Session.", IO.TAG_ERROR);
                                            return;
                                        }
                                        if(SessionManager.getInstance().getActive().isExpired())
                                        {
                                            IO.logAndAlert("Error", "Active Session has expired.", IO.TAG_ERROR);
                                            return;
                                        }

                                        Quote quote = new Quote();
                                        quote.setEnquiry_id(enquiry.get_id());
                                        quote.setVat(QuoteManager.VAT);
                                        quote.setStatus(Quote.STATUS_PENDING);
                                        quote.setAccount_name(enquiry.getCreatorUser().getOrganisationName().toLowerCase());//TODO: get Organisation object and getName()
                                        quote.setRequest(enquiry.getEnquiry());
                                        quote.setClient_id(enquiry.getCreatorUser().getOrganisation_id());
                                        quote.setContact_person_id(enquiry.getCreator());
                                        quote.setCreator(SessionManager.getInstance().getActiveUser().getUsr());
                                        quote.setRevision(1.0);

                                        List<User> quote_reps = new ArrayList<>();
                                        try
                                        {
                                            QuoteManager.getInstance().createQuote(quote, null, new Callback()
                                            {
                                                @Override
                                                public Object call(Object new_quote_id)
                                                {
                                                    ScreenManager.getInstance().showLoadingScreen(arg ->
                                                    {
                                                        new Thread(new Runnable()
                                                        {
                                                            @Override
                                                            public void run()
                                                            {
                                                                //set selected Quote
                                                                QuoteManager.getInstance().setSelectedQuote((String)new_quote_id);
                                                                try
                                                                {
                                                                    if(ScreenManager.getInstance().loadScreen(Screens.VIEW_QUOTE.getScreen(),mvg.MVG.class.getResource("views/"+Screens.VIEW_QUOTE.getScreen())))
                                                                    {
                                                                        Platform.runLater(() -> ScreenManager.getInstance().setScreen(Screens.VIEW_QUOTE.getScreen()));
                                                                    }
                                                                    else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load Quotes viewer screen.");
                                                                } catch (IOException e)
                                                                {
                                                                    e.printStackTrace();
                                                                    IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                                                }
                                                            }
                                                        }).start();
                                                        return null;
                                                    });
                                                    return null;
                                                }
                                            });
                                        } catch (IOException e)
                                        {
                                            e.printStackTrace();
                                            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                        }
                                    });

                                    btnRemove.setOnAction(event ->
                                    {
                                        //Quote quote = getTableView().getItems().get(getIndex());
                                        getTableView().getItems().remove(enquiry);
                                        getTableView().refresh();
                                        //TODO: remove from server
                                        //IO.log(getClass().getName(), IO.TAG_INFO, "successfully removed quote: " + quote.get_id());
                                    });

                                    hBox.setFillHeight(true);
                                    HBox.setHgrow(hBox, Priority.ALWAYS);
                                    hBox.setSpacing(5);
                                    setGraphic(hBox);
                                    setText(null);
                                }
                            }
                        };
                        return cell;
                    }
                };

        colAction.setCellValueFactory(new PropertyValueFactory<>(""));
        colAction.setCellFactory(cellFactory);

        tblEnquiries.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) ->
                EnquiryManager.getInstance().setSelectedEnquiry(tblEnquiries.getSelectionModel().getSelectedItem()));
    }

    @Override
    public void refreshModel()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading enquiries data model..");
        try
        {
            EnquiryManager.getInstance().reloadDataFromServer();
        } catch (ClassNotFoundException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        refreshModel();
        refreshView();
    }
}
