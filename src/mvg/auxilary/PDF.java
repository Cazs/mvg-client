package mvg.auxilary;

import mvg.model.Enquiry;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.Encoding;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class PDF
{
    private static final int LINE_HEIGHT=20;
    private static final int ROW_COUNT = 35;
    private static final int TEXT_VERT_OFFSET=LINE_HEIGHT/4;
    private static final Insets page_margins = new Insets(100,10,100,10);

    private static void drawHorzLines(PDPageContentStream contents, int y_start, int page_width, Insets offsets) throws IOException
    {
        contents.setStrokingColor(new Color(171, 170, 166));
        //horizontal top title underline
        contents.moveTo(offsets.left, y_start);
        contents.lineTo(page_width-offsets.right, y_start);
        contents.stroke();
        for(int i=y_start;i>offsets.bottom;i-=LINE_HEIGHT)
        {
            //horizontal underline
            contents.moveTo(offsets.left, i-LINE_HEIGHT);
            contents.lineTo(page_width-offsets.right, i-LINE_HEIGHT);
            contents.stroke();
            //line_pos-=LINE_HEIGHT;
        }
    }

    private static void drawVertLines(PDPageContentStream contents, int[] x_positions, int y_start) throws IOException
    {
        for(int x: x_positions)
        {
            contents.moveTo(x, y_start);
            contents.lineTo(x, page_margins.bottom);
            contents.stroke();
        }
    }

    public static void createBordersOnPage(PDPageContentStream contents, int page_w, int page_top, int page_bottom) throws IOException
    {
        //top border
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, page_top);
        contents.lineTo(page_w-10, page_top);
        contents.stroke();
        //left border
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, page_top);
        contents.lineTo(10, page_bottom-LINE_HEIGHT);
        contents.stroke();
        //right border
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(page_w-10, page_top);
        contents.lineTo(page_w-10, page_bottom-LINE_HEIGHT);
        contents.stroke();
        //bottom border
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, page_bottom-LINE_HEIGHT);
        contents.lineTo(page_w-10, page_bottom-LINE_HEIGHT);
        contents.stroke();
    }

    public static void createLinesAndBordersOnPage(PDPageContentStream contents, int page_w, int page_top, int page_bottom) throws IOException
    {
        boolean isTextMode=false;
        try
        {//try to end the text of stream.
            contents.endText();
            isTextMode=true;
        }catch (IllegalStateException e) {}
        //draw borders
        createBordersOnPage(contents, page_w, page_top, page_bottom);
        //draw horizontal lines
        int line_pos=page_top;
        for(int i=0;i<ROW_COUNT;i++)//35 rows
        {
            //horizontal underline
            contents.setStrokingColor(new Color(171, 170, 166));
            contents.moveTo(10, line_pos-LINE_HEIGHT);
            contents.lineTo(page_w-10, line_pos-LINE_HEIGHT);
            contents.stroke();
            line_pos-=LINE_HEIGHT;
        }
        if(isTextMode)
            contents.beginText();
    }

    public static void addTextToPageStream(PDPageContentStream contents, String text, int font_size, int x, int y) throws IOException
    {
        try
        {
            addTextToPageStream(contents, text, PDType1Font.HELVETICA, font_size, x, y);
        }catch (IllegalArgumentException e)
        {
            IO.log("PDF creator", IO.TAG_ERROR, e.getMessage());
        }
    }

    public static void addTextToPageStream(PDPageContentStream contents, String text, PDFont font,int font_size, int x, int y) throws IOException
    {
        contents.setFont(font, font_size);
        contents.setTextMatrix(new Matrix(1, 0, 0, 1, x, y-TEXT_VERT_OFFSET));

        char[] text_arr = text.toCharArray();
        StringBuilder str_builder = new StringBuilder();
        //PDType0Font.
        Encoding e = org.apache.pdfbox.pdmodel.font.encoding.Encoding.getInstance(COSName.WIN_ANSI_ENCODING);// EncodingManager.INSTANCE.getEncoding(COSName.WIN_ANSI_ENCODING);
        //Encoding e = EncodingManager.INSTANCE.getEncoding(COSName.WIN_ANSI_ENCODING);

        System.out.println("\n\n::::::::::::::::::::Processing Text: [" + text + "]::::::::::::::::::::");
        System.out.println("Encoding Name: " + e.getEncodingName());
        System.out.println("Encoding Name to Code Map: " + e.getNameToCodeMap());
        //String toPDF = String.valueOf(Character.toChars(e.getCode(e.getNameFromCharacter(symbol))));

        for (int i = 0; i < text_arr.length; i++)
        {
            Character c = text_arr[i];
            int code = 0;
            System.out.println(String.format("Character [%s] has codename: [%s] and code [%s]", c, e.getName(c), String.valueOf(e.getNameToCodeMap().get(c))));
            if(e.getName(c).toLowerCase().equals(".notdef"))
                str_builder.append("[?]");
            else str_builder.append(c);
            /*if(Character.isWhitespace(c))
            {
                code = e.getNameToCodeMap().get("space");
            }else{
                String toPDF = String.valueOf(Character.toChars(e.getCodeToNameMap().get(e.getName(symbol))));
                code = e.getNameToCodeMap(e.getName(c));
            }
            str_builder.appendCodePoint(code);*/
        }
        contents.showText(str_builder.toString());
    }

    public static String createEnquiryPDF(Enquiry enquiry) throws IOException
    {
        //create PDF output directory
        if(new File("out/pdf/").mkdirs())
            IO.log(PDF.class.getName(), "successfully created PDF output directory [out/pdf/]", IO.TAG_INFO);

        // Create a new document with an empty page.
        final PDDocument document = new PDDocument();
        final PDPage page = new PDPage(PDRectangle.A4);

        final float w = page.getBBox().getWidth();
        final float h = page.getBBox().getHeight();

        //Add page to document
        document.addPage(page);

        // Adobe Acrobat uses Helvetica as a default font and
        // stores that under the name '/Helv' in the resources dictionary
        PDFont font = PDType1Font.HELVETICA;
        PDResources resources = new PDResources();
        resources.put(COSName.getPDFName("Helv"), font);

        PDPageContentStream contents = new PDPageContentStream(document, page);
        int logo_h = 60;
        PDImageXObject logo = PDImageXObject.createFromFile("images/logo.png", document);
        contents.drawImage(logo, (w/2)-80, 770, 160, logo_h);

        int line_pos = (int)h-logo_h-LINE_HEIGHT;

        /** draw horizontal lines **/
        drawHorzLines(contents, line_pos, (int)w, page_margins);
        /** draw vertical lines **/
        final int[] col_positions = {(int)((w / 2)), (int)((w / 2) + 100), (int)((w / 2) + 200)};
        drawVertLines(contents, col_positions, line_pos-LINE_HEIGHT);
        line_pos = (int)h-logo_h-LINE_HEIGHT;

        /** begin text from the top**/
        contents.beginText();
        contents.setFont(font, 12);
        line_pos-=10;
        //Heading text
        addTextToPageStream(contents, "Enquiry", 16,(int)(w/2)-70, line_pos);
        line_pos-=LINE_HEIGHT*2;//next 2nd line

        addTextToPageStream(contents, "DATE LOGGED: ", 16,10, line_pos);
        addTextToPageStream(contents, (new SimpleDateFormat("yyyy-MM-dd").format(enquiry.getDate_logged()*1000)), 16,(int)w/2+100, line_pos);
        line_pos-=LINE_HEIGHT*2;//next 2nd line

        addTextToPageStream(contents, enquiry.getEnquiry(), PDType1Font.TIMES_ITALIC, 16 ,10, line_pos);
        line_pos-=LINE_HEIGHT*2;//next 2nd line

        //Create column headings
        addTextToPageStream(contents,"Trip Type", 14,10, line_pos);
        addTextToPageStream(contents,"From", 14, col_positions[0]+10, line_pos);
        addTextToPageStream(contents,"To", 14,col_positions[1]+10, line_pos);
        addTextToPageStream(contents,"Date", 14,col_positions[2]+10, line_pos);

        contents.endText();
        line_pos-=LINE_HEIGHT;//next line

        //int pos = line_pos;
        contents.beginText();
        addTextToPageStream(contents, String.valueOf(enquiry.getTrip_type()), 14, 10, line_pos);
        addTextToPageStream(contents, enquiry.getPickup_location(), 12, col_positions[0]+5, line_pos);
        addTextToPageStream(contents, enquiry.getDestination(), 12, col_positions[1]+5, line_pos);
        if(enquiry.getDate_scheduled()>0)
            addTextToPageStream(contents, (new SimpleDateFormat("yyyy-MM-dd").format(enquiry.getDate_scheduled()*1000)), 12, col_positions[2]+5, line_pos);
        else addTextToPageStream(contents, "N/A", 12, col_positions[2]+5, line_pos);

        line_pos-=LINE_HEIGHT*2;//next 2nd line
        String status = "N/A";
        /*switch (leave.getStatus())
        {
            case Leave.STATUS_PENDING:
                status="PENDING";
                break;
            case Leave.STATUS_APPROVED:
                status="GRANTED";
                break;
            case Leave.STATUS_ARCHIVED:
                status="ARCHIVED";
                break;
        }
        addTextToPageStream(contents, "STATUS: ", 14,10, line_pos);
        addTextToPageStream(contents, status, 14,100, line_pos);*/
        line_pos-=LINE_HEIGHT*2;//next 2nd line

        if(enquiry.getOther()!=null)
            addTextToPageStream(contents, enquiry.getOther(), 16, 15, line_pos);

        line_pos-=LINE_HEIGHT*3;//next 3rd line
        addTextToPageStream(contents, "Applicant's Signature", 16,10, line_pos);
        addTextToPageStream(contents, "Manager Signature", 16, 200, line_pos);
        contents.endText();

        //draw first signature line
        contents.setStrokingColor(Color.BLACK);
        contents.moveTo(10, line_pos+LINE_HEIGHT+5);
        contents.lineTo(120, line_pos+LINE_HEIGHT+5);
        contents.stroke();
        //draw second signature line
        contents.moveTo(200, line_pos+LINE_HEIGHT+5);
        contents.lineTo(320, line_pos+LINE_HEIGHT+5);
        contents.stroke();

        String path = "out/pdf/enquiry_" + enquiry.get_id() + ".pdf";
        int i=1;
        while(new File(path).exists())
        {
            path = "out/pdf/enquiry_" + enquiry.get_id() + "." + i + ".pdf";
            i++;
        }

        contents.close();
        document.save(path);
        document.close();

        return path;
    }
}