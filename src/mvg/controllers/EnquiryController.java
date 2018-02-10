package mvg.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import mvg.auxilary.IO;
import mvg.auxilary.Validators;
import mvg.managers.*;
import mvg.model.CustomTableViewControls;
import mvg.model.Enquiry;
import mvg.model.Quote;
import mvg.model.Screens;

import java.io.IOException;
import java.net.URL;
import java.time.ZoneId;
import java.util.ResourceBundle;

public class EnquiryController extends ScreenController implements Initializable
{
    @FXML
    private TextField txtEnquiry, txtTime, txtAddress, txtDestination, txtTripType, txtComments;
    @FXML
    private DatePicker dateScheduled;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        refreshModel();
        Platform.runLater(() -> refreshView());
    }

    @Override
    public void refreshView()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading enquiries view..");

        if(UserManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert(getClass().getSimpleName(), "No users were found in the database.", IO.TAG_ERROR);
            return;
        }
        txtEnquiry.setText(HomescreenController.selected_enquiry);
    }

    @Override
    public void refreshModel()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading enquiry data model..");

    }

    @FXML
    public void submitEnquiry()
    {
        if(SessionManager.getInstance().getActiveUser()==null)
        {
            IO.logAndAlert("Error: Invalid Session", "Active session is invalid.\nPlease log in.", IO.TAG_ERROR);
            return;
        }
        if(SessionManager.getInstance().getActive()==null)
        {
            IO.logAndAlert("Error: Invalid Session", "Active session is invalid.\nPlease log in.", IO.TAG_ERROR);
            return;
        }
        if(SessionManager.getInstance().getActive().isExpired())
        {
            IO.logAndAlert("Error: Invalid Session", "Active session has expired.\nPlease log in.", IO.TAG_ERROR);
            return;
        }
        if(!Validators.isValidNode(txtEnquiry, "Invalid Enquiry", 5, "^.*(?=.{5,}).*"))//"please enter a valid enquiry"
            return;
        if(!Validators.isValidNode(dateScheduled, (dateScheduled.getValue()==null?"":String.valueOf(dateScheduled.getValue())), "^.*(?=.{1,}).*"))
            return;
        if(!Validators.isValidNode(txtTime, "Invalid Pickup Location", 5, "^.*(?=.{1,}).*"))//"please enter a valid pickup address"
            return;
        if(!Validators.isValidNode(txtAddress, "Invalid Pickup Location", 1, "^.*(?=.{1,}).*"))//"please enter a valid pickup address"
            return;
        if(!Validators.isValidNode(txtDestination, "Invalid Destination", 1, "^.*(?=.{1,}).*"))//"please enter a valid destination"
            return;
        if(!Validators.isValidNode(txtTripType, "Invalid Trip Type", 1, "^.*(?=.{1,}).*"))//"please enter a valid trip type"
            return;

        Enquiry enquiry = new Enquiry();
        enquiry.setEnquiry(txtEnquiry.getText());
        enquiry.setClient_id(SessionManager.getInstance().getActiveUser().getOrganisation_id());
        enquiry.setComments(txtComments.getText());
        enquiry.setPickup_location(txtAddress.getText());
        enquiry.setDestination(txtDestination.getText());
        enquiry.setDate_scheduled(dateScheduled.getValue().atStartOfDay(ZoneId.systemDefault()).toEpochSecond());
        enquiry.setTrip_type(txtTripType.getText());
        enquiry.setCreator(SessionManager.getInstance().getActive().getUsername());

        try
        {
            EnquiryManager.getInstance().createEnquiry(enquiry, new_enquiry_id ->
            {
                IO.logAndAlert("Success", "Created Enquiry ["+new_enquiry_id+"].", IO.TAG_INFO);
                return null;
            });
        } catch (IOException e)
        {
            IO.logAndAlert("I/O Error", e.getMessage(), IO.TAG_ERROR);
        }
    }
}
