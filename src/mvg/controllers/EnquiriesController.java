package mvg.controllers;

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
import mvg.managers.EnquiryManager;
import mvg.managers.EnquiryManager;
import mvg.managers.ScreenManager;
import mvg.managers.SessionManager;
import mvg.model.CustomTableViewControls;
import mvg.model.Enquiry;
import mvg.model.Screens;
import mvg.model.User;

import java.io.IOException;
import java.net.URL;
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
        CustomTableViewControls.makeLabelledDatePickerTableColumn(colDate, "date_scheduled", "/enquiries");
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
                                    Enquiry client = getTableView().getItems().get(getIndex());

                                    btnQuote.setOnAction(event ->
                                    {
                                        //System.out.println("Successfully added material quote number " + quoteItem.getItem_number());
                                        //EnquiryManager.getInstance().setSelected(client);
                                        //screenManager.setScreen(Screens.VIEW_JOB.getScreen());
                                    });

                                    btnRemove.setOnAction(event ->
                                    {
                                        //Quote quote = getTableView().getItems().get(getIndex());
                                        getTableView().getItems().remove(client);
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
