package mvg.model;

import mvg.auxilary.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;

/**
 * Created by ghost on 2017/01/11.
 */
public class CustomTableViewControls
{
    public static final String TAG = "CustomTableViewControls";

    public static void makeDatePickerTableColumn(TableColumn<BusinessObject, Long> date_col, String property, String api_method)
    {
        date_col.setMinWidth(130);
        date_col.setCellValueFactory(new PropertyValueFactory<>(property));
        date_col.setCellFactory(col -> new DatePickerCell(property, api_method));
        date_col.setEditable(true);
        date_col.setOnEditCommit(event -> event.getRowValue().parse(property, event.getNewValue()));
    }

    public static void makeLabelledDatePickerTableColumn(TableColumn<BusinessObject, Long> date_col, String property, String api_method)
    {
        date_col.setMinWidth(130);
        date_col.setCellValueFactory(new PropertyValueFactory<>(property));
        date_col.setCellFactory(col -> new LabelledDatePickerCell(property, api_method));
        date_col.setEditable(true);
        date_col.setOnEditCommit(event -> event.getRowValue().parse(property, event.getNewValue()));
    }

    public static void makeDatePickerTableColumn(TableColumn<BusinessObject, Long> date_col, String property, boolean editable)
    {
        date_col.setMinWidth(130);
        date_col.setCellValueFactory(new PropertyValueFactory<>(property));
        date_col.setCellFactory(col -> new DatePickerCell(property, editable));
        date_col.setEditable(false);
        date_col.setOnEditCommit(event -> event.getRowValue().parse(property, event.getNewValue()));
    }

    public static void makeEditableTableColumn(TableColumn<BusinessObject, String> col, Callback<TableColumn<BusinessObject, String>, TableCell<BusinessObject, String>> editable_control_callback, int min_width, String property, String api_call)
    {
        if(col!=null)
        {
            col.setMinWidth(min_width);
            col.setCellValueFactory(new PropertyValueFactory<>(property));
            col.setCellFactory(editable_control_callback);
            col.setOnEditCommit(event ->
            {
                BusinessObject bo = event.getRowValue();
                if(bo!=null)
                {
                    bo.parse(property, event.getNewValue());
                    RemoteComms.updateBusinessObjectOnServer(bo, api_call, property);
                }
            });
        }else{
            IO.log(TAG, IO.TAG_ERROR, "Null table column!");
        }
    }

    public static void makeEditableColumn(TableColumn<BusinessObject, String> col, Callback<TableColumn<BusinessObject, String>, TableCell<BusinessObject, String>> editable_control_callback, int min_width, String property, String api_call)
    {
        if(col!=null)
        {
            col.setMinWidth(min_width);
            col.setCellValueFactory(new PropertyValueFactory<>(property));
            col.setCellFactory(editable_control_callback);
            col.setOnEditCommit(event ->
            {
                BusinessObject bo = event.getRowValue();
                if(bo!=null)
                {
                    bo.parse(property, event.getNewValue());
                    RemoteComms.updateBusinessObjectOnServer(bo, api_call, property);
                }
            });
        }else{
            IO.log(TAG, IO.TAG_ERROR, "Null table column!");
        }
    }

    public static void createEditableTableColumn(TableColumn<BusinessObject, String> col, Callback<TableColumn<BusinessObject, String>, TableCell<BusinessObject, String>> editable_control_callback, int min_width, String property, String api_call)
    {
        if(col!=null)
        {
            col.setMinWidth(min_width);
            col.setCellValueFactory(new PropertyValueFactory<>(property));
            col.setCellFactory(editable_control_callback);
            col.setOnEditCommit(event ->
            {
                BusinessObject bo = event.getRowValue();
                if(bo!=null)
                    bo.parse(property, event.getNewValue());
            });
        }else{
            IO.log(TAG, IO.TAG_ERROR, "Null table column!");
        }
    }

    public static void makeCheckboxedTableColumn(TableColumn<BusinessObject, GridPane> col, Callback<TableColumn<BusinessObject, GridPane>, TableCell<BusinessObject,GridPane>> editable_control_callback, int min_width, String property, String api_call)
    {
        if (col != null)
        {
            col.setMinWidth(min_width);
            //col.setCellFactory(editable_control_callback);
            col.setCellValueFactory((TableColumn.CellDataFeatures<BusinessObject, GridPane> param) ->
            {
                BusinessObject bo = param.getValue();

                CheckBox cbx = new CheckBox();
                GridPane grid = new GridPane();
                grid.setAlignment(Pos.CENTER);

                cbx.setAlignment(Pos.CENTER);
                grid.add(cbx, 0, 0);
                if(property.toLowerCase().equals("marked"))
                {
                    bo.setMarked(false);
                }
                cbx.selectedProperty().addListener((observable, oldValue, newValue) ->
                        bo.setMarked(newValue));
                return new SimpleObjectProperty<>(grid);
            });
        } else
        {
            IO.log(TAG, IO.TAG_ERROR, "Null table column!");
        }
    }

    public static HBox getLabelledNode(String label, int lbl_min_w, Node node)
    {
        HBox hbox = new HBox();
        Label lbl = new Label(label);
        HBox.setMargin(lbl, new Insets(8));
        hbox.getChildren().add(lbl);
        lbl.setMinWidth(lbl_min_w);

        //node.minWidth(node_min_w);
        HBox.setHgrow(node, Priority.ALWAYS);
        hbox.getChildren().add(node);
        VBox.setMargin(hbox, new Insets(0,10,0,10));

        HBox.setHgrow(hbox, Priority.ALWAYS);
        return hbox;
    }

    public static HBox getLabelledNode(String label, int lbl_min_w, Node node, Color label_colour)
    {
        HBox hbox = new HBox();
        Label lbl = new Label(label);
        lbl.setTextFill(label_colour);
        HBox.setMargin(lbl, new Insets(8));
        hbox.getChildren().add(lbl);
        lbl.setMinWidth(lbl_min_w);

        //node.minWidth(node_min_w);
        HBox.setHgrow(node, Priority.ALWAYS);
        hbox.getChildren().add(node);
        VBox.setMargin(hbox, new Insets(0,10,0,10));

        HBox.setHgrow(hbox, Priority.ALWAYS);
        return hbox;
    }

    public static HBox getSpacedButton(String btn_name, EventHandler<ActionEvent> btnClickHandler)
    {
        HBox name = new HBox();
        Label lbl = new Label("");
        HBox.setMargin(lbl, new Insets(5));
        name.getChildren().add(lbl);
        lbl.setMinWidth(150);

        Button btn = new Button(btn_name);
        btn.setMinWidth(150);
        btn.setMinHeight(50);
        btn.getStyleClass().add("btnDefault");
        HBox.setHgrow(btn, Priority.ALWAYS);
        btn.setMaxWidth(400);
        btn.setMaxHeight(100);
        name.getChildren().add(btn);

        btn.setOnAction(btnClickHandler);

        return name;
    }

    public static boolean validateFormField(TextField txt, String errTitle, String errMsg, String regex)
    {
        if(!Validators.isValidNode(txt, txt.getText(), regex))
        {
            //IO.logAndAlert(errTitle, errMsg, IO.TAG_ERROR);
            IO.log(CustomTableViewControls.class.getName(), IO.TAG_ERROR, errMsg);
            return false;
        }
        return true;
    }
}
