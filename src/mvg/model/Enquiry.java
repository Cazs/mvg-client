/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import mvg.auxilary.IO;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 *
 * @author ghost
 */
public class Enquiry implements BusinessObject, Serializable
{
    private String _id;
    private String enquiry;
    private String pickup_location;
    private String destination;
    private String trip_type;
    private String comments;
    private long date_scheduled;
    private boolean marked;
    public static final String TAG = "Enquiry";

    public StringProperty idProperty(){return new SimpleStringProperty(_id);}

    @Override
    public String get_id()
    {
        return _id;
    }

    public void set_id(String _id)
    {
        this._id = _id;
    }

    public StringProperty short_idProperty(){return new SimpleStringProperty(_id.substring(0, 8));}

    @Override
    public String getShort_id()
    {
        return _id.substring(0, 8);
    }

    private StringProperty enquiryProperty(){return new SimpleStringProperty(enquiry);}

    public String getEnquiry()
    {
        return enquiry;
    }

    public void setEnquiry(String enquiry) {
        this.enquiry = enquiry;
    }

    private StringProperty pickup_locationProperty(){return new SimpleStringProperty(pickup_location);}

    public String getPickup_location()
    {
        return pickup_location;
    }

    public void setPickup_location(String password) {
        this.pickup_location = pickup_location;
    }

    private StringProperty destinationProperty(){return new SimpleStringProperty(String.valueOf(destination));}

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination)
    {
        this.destination = destination;
    }

    private StringProperty trip_typeProperty(){return new SimpleStringProperty(String.valueOf(trip_type));}

    public String getTrip_type()
    {
        return String.valueOf(trip_type);
    }

    public void setTrip_type(String trip_type)
    {
        this.trip_type = trip_type;
    }

    private StringProperty commentsProperty(){return new SimpleStringProperty(comments);}

    public String getComments()
    {
        return comments;
    }

    public void setComments(String comments)
    {
        this.comments = comments;
    }

    private StringProperty date_scheduledProperty(){return new SimpleStringProperty(String.valueOf(getDate_scheduled()));}

    public long getDate_scheduled()
    {
        return date_scheduled;
    }

    public void setDate_scheduled(long date_scheduled)
    {
        this.date_scheduled = date_scheduled;
    }

    @Override
    public void parse(String var, Object val)
    {
        try
        {
            switch (var.toLowerCase())
            {
                case "enquiry":
                    setEnquiry((String)val);
                    break;
                case "date_scheduled":
                    setDate_scheduled(Long.parseLong((String)val));
                    break;
                case "pickup_location":
                    setPickup_location((String)val);
                    break;
                case "destination":
                    setDestination((String)val);
                    break;
                case "trip_type":
                    setTrip_type((String)val);
                    break;
                case "comments":
                    setComments((String)val);
                    break;
                default:
                    IO.log(TAG, IO.TAG_WARN, String.format("unknown Enquiry attribute '%s'", var));
                    break;
            }
        }catch (NumberFormatException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @Override
    public Object get(String var)
    {
        switch (var.toLowerCase())
        {
            case "enquiry":
                return enquiry;
            case "date_scheduled":
                return date_scheduled;
            case "pickup_location":
                return pickup_location;
            case "destination":
                return destination;
            case "trip_type":
                return trip_type;
            case "comments":
                return comments;
            default:
                IO.log(TAG, IO.TAG_WARN, String.format("unknown Enquiry attribute '%s'", var));
                return null;
        }
    }

    @Override
    public boolean isMarked()
    {
        return marked;
    }

    @Override
    public void setMarked(boolean marked)
    {
        this.marked=marked;
    }

    @Override
    public String toString()
    {
        return enquiry;
    }

    @Override
    public String asUTFEncodedString()
    {
        //Return encoded URL parameters in UTF-8 charset
        StringBuilder result = new StringBuilder();
        try
        {
            result.append(URLEncoder.encode("enquiry","UTF-8") + "="
                    + URLEncoder.encode(enquiry, "UTF-8") + "&");
            result.append(URLEncoder.encode("pickup_location","UTF-8") + "="
                    + URLEncoder.encode(pickup_location, "UTF-8") + "&");
            if(date_scheduled>0)
                result.append(URLEncoder.encode("date_scheduled","UTF-8") + "="
                        + URLEncoder.encode(String.valueOf(date_scheduled), "UTF-8") + "&");
            result.append(URLEncoder.encode("firstname","UTF-8") + "="
                    + URLEncoder.encode(destination, "UTF-8") + "&");
            result.append(URLEncoder.encode("trip_type","UTF-8") + "="
                    + URLEncoder.encode(trip_type, "UTF-8") + "&");
            result.append(URLEncoder.encode("comments","UTF-8") + "="
                    + URLEncoder.encode(comments, "UTF-8") + "&");
            return result.toString();
        } catch (UnsupportedEncodingException e)
        {
            IO.log(TAG, IO.TAG_ERROR, e.getMessage());
        }
        return null;
    }

    @Override
    public String apiEndpoint()
    {
        return "/api/employee";
    }
}
