package test.edu.upenn.cis455;

import indexer.Indexer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.l3s.boilerpipe.BoilerpipeProcessingException;

import junit.framework.TestCase;

public class IndexerTest extends TestCase {
	public void testExtractTestFromHtmlEasy() throws IOException, BoilerpipeProcessingException {
		File file = new File("/home/cis455/Desktop/easyHtml.htm");
		FileReader fr = new FileReader(file);
		char[] cbuf = new char[1024];
		int bytesRead;
		String input = "";
		do{
		bytesRead = fr.read(cbuf);
		if(bytesRead!=-1){
			input += String.valueOf(cbuf, 0, bytesRead);
		}
		}while(bytesRead==1024);
		//System.out.print(Indexer.extractTextFromHtml(input));
		fr.close();
		
	}
	
	/*public void testLanguageDetection() throws LangDetectException, IOException {
		DetectorFactory.loadProfile(new File("src/profiles"));
		Detector detector = DetectorFactory.create();
		detector.append("初期的成功及");
		System.out.println(detector.detect());
	}*/
	
	public void testExtractTextFromXml() throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilder d = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Indexer.extractTextFromXml(d.parse("http://www.w3schools.com/dom/books.xml"));
	}
	
	public void testExtractTextFromPdf() throws IOException{
		RandomAccessFile file = new RandomAccessFile("/home/cis455/Desktop/ORDER_CONFIRMATION.pdf", "r");
		byte[] byteArr = new byte[(int) file.length()];
		try {
			file.readFully(byteArr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(Indexer.extractTextFromPdf(byteArr));
		assertEquals("Hello Angela,\n" +
				"Thank you for choosing Door to Door. This Confirmation Email includes your order details, payment summary and links to our Services \n" +
				"Agreement Terms and Conditions.  Please review these documents as they contain important information regarding your order. We look \n" +
				"forward to providing you with the services described below, and we are committed to making your experience an exceptional one.\nIMPORTANT - PLEASE READ FIRST!" +
				"\nWe must receive your acceptance of the Services Agreement Terms and Conditions prior to delivery of your containers. \n" +
				"Please click on the link below to log in, review and accept the Terms and Conditions.\n" +
				"Terms and Conditions\n" +
				"Please also review important information describing your delivery requirements, container placement and fees, at:\n" +
				"Doing Business with Door to Door\n" +
				"Are you Moving to California? As required by the State of California, we must receive your completed \n\"" +
				"Gyspy Moth Document\" before we can ship your belongings.  Click the link below to find the forms and instructions.  \n" +
				"Please return to us before your initial delivery date to avoid delays in shipping and redelivery dates.\n" +
				"Gypsy Moth Document\nCustomer Name: Angela Wu\n" +
				"Customer Address: 3600 Chestnut Street\nPhiladelphia, PA 19104\nNumber of Containers: 1\nWork Order # Date Service Type Service Address\n 126761 PHILADELPHIA: Storage - Begin\n05/04/2012 PHILADELPHIA: Initial Delivery ZoneB 3600 Chestnut Street  Philadelphia, PA 19104\n05/08/2012 PHILADELPHIA: PickUp Full ZoneB 923 N. Lenola Rd.  Moorestown, NJ 08057\nPayment Summary:\nDescription Quantity Amount Tax Total\nSMRPROMO:Summer Storage Promotion  1 $395.00 $27.65 $422.65 \nPERMIT:Storage-Permit Fee  1 $75.00 $0.00 $75.00 \nSMRPROMORNT: Summer Promo  1 ($69.00) ($4.83) ($73.83)\nRent for unit:  1 $69.00 $4.83 $73.83 \nTotal Due: $497.65 \nTotal Due to Door to Door will be collected prior to the initial delivery of containers.  If you are an existing storage customer and \nmoving, the Total Due for your move will be collected prior to shipment of containers. \nStorage Promotion Package: If your order involves one of our storage promotions, AutoPay enrollment is required. If your storage \nneeds extend beyond the contract term, you can remain in storage until you are ready to close out your order. Standard monthly storage \nrates may apply after the contract ends, or you may call us to extend your contract and discuss lower rate options. If you choose to \nterminate or cancel your storage service before your contract ends, you will be responsible for the balance of the contract.\nAutoPay Enrollment: Door to Door has a convenient AutoPay program that automatically charges your credit or debit card monthly for \nrecurring charges, as applicable. Should a customer choose not to enroll, a $50.00 per container security deposit will be charged and the \nstandard monthly storage rates will apply.\n*Estimated Date of Arrival at Destination Storage Center - for Moving Customers Only:  Due to conditions outside of Door to \nDoor\u2019s control (e.g., weather, holidays, road conditions, equipment failure), the Receive at Warehouse date is an estimated date that your \npossessions will arrive at the destination storage center. The actual Receive at Warehouse date may vary several business days.  Please \nkeep this in mind during your planning. If your plans require a guaranteed date, please call us back to obtain priority moving pricing and \nscheduling. The final delivery of your container(s) will be coordinated by the destination storage center once your container(s) are \nreceived. Door to Door makes every effort to communicate changes in advance.\nCustomer Protection Plan (CPP) Valuation: If you elect to participate in the CPP, Door to Door\u2019s liability is limited to a maximum of \nseven thousand dollars ($7,000.00) per container, less the $250 deductible, as determined by an independent claims adjuster. The CPP \ncoverage is based on the actual cash value of the items at the time of loss or damage (i.e., the depreciated value). If CPP is elected, you \nagree that liability for any and all value above the $7,000.00 maximum is solely your responsibility.  Please reference the Services \nAgreement Terms and Conditions for more information regarding CPP.\nAppointment Changes/Cancellation Policy: We recognize that your plans may change, and we want to accommodate those adjustments \nwhenever possible. If you change dates for container delivery or pick-up with less than two (2) business days notice, Door to Door \nreserves the right to change the delivery dates in this confirmation. If you cancel your reservation seven (7) or more days prior to your \nscheduled delivery date, you will receive a full deposit refund. If you cancel in less than seven (7) days prior to your delivery date, you \nwill forfeit your deposit. If you cancel when the driver arrives at your location, the delivery fee as well as the deposit will be charged. \nAdditionally, any supplies that are ordered and shipped to you in advance of your container delivery are non-refundable, and associated \ncharges will be collected at the time of cancellation.\nAdditional Boxes, Blankets and Supplies: A full assortment of packing supplies, boxes, blankets/pads and furniture covers is available \nand can be delivered with your containers. If you would prefer to start packing before container delivery, supplies can be mailed directly \nto you with free shipping. A complete selection is available at www.doortodoor.com/boxes.\nContainer Usage: You may change the number of containers ordered and will be charged only for the containers you actually use. \nAdditional containers, for a total of up to five (5) containers, can be ordered up to two (2) business days prior to delivery without an \nadditional delivery fee. Container orders that exceed five (5) total containers, or orders placed within two (2) business days of delivery, \nwill include an additional delivery fee.\nIndependent Service Providers:  To provide customers with more options, Door to Door offers moving services in several areas across \nthe country through coordination with Independent Service Providers.  These Independent Service Providers offer similar services to \nDoor to Door\u2019s services, but might vary slightly.  The Independent Service Provider will confirm their scheduled appointment dates with \nyou separately.  Please note, if the Independent Service Provider\u2019s appointment date(s) change, this may change your shipping and \nredelivery dates.  Door to Door makes every effort to communicate changes in advance.\nOur goal is to make this a convenient and successful experience for you. If any of our Door to Door team can be of further assistance, \nplease contact us.\n \nThank you, \nDoor to Door Storage, Inc.\nwww.doortodoor.com\n1-888-366-7222, Option 1\n \n", Indexer.extractTextFromPdf(byteArr));		
		file.close();
	}
}
