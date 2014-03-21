package ch.unifr.freiburgnet.ws;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;

import net.xqj.basex.BaseXXQDataSource;

import org.xml.sax.SAXException;

//Plain old Java Object it does not extend as class or implements 
//an interface

//The class registers its methods for the HTTP POST request using the @POST annotation. 
//Using the @Consumes annotation, it defines that it can accept XML MIME type.

@Path("/plans")
public class PlanService {

	@Context
	UriInfo uriInfo;

	@Context
	Request request;

	// This method is called if TEXT_PLAIN is request
	@POST
	@Path("{id}")
	@Consumes(MediaType.TEXT_XML)
	public Response sayPlainTextHello(@PathParam("id") String id,String xml) throws IOException {

		URL schemaFile = new URL(uriInfo.getBaseUri().toURL(),"../static/plan.xsd");

		Source xmlFile = new StreamSource(new StringReader(xml));
		
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema;
		try {
			schema = schemaFactory.newSchema(schemaFile);
		} catch (SAXException e) {
			return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(e.getLocalizedMessage()).build();
		}

		Validator validator = schema.newValidator();
		try {
			validator.validate(xmlFile);
		} catch (SAXException e) {
			return Response.status(Response.Status.NOT_ACCEPTABLE).entity(e.getLocalizedMessage()).build();
		}

		try {
			XQDataSource xqs = new BaseXXQDataSource();

			xqs.setProperty("serverName", "localhost");
			xqs.setProperty("port", "1984");

			XQConnection conn = xqs.getConnection("admin", "admin");		

			XQExpression xqe = conn.createExpression();
			//xqe.executeCommand("SET WRITEBACK true");
			
			xqe.bindString(new QName("id"), id, null);

			String query = "declare variable $id as xs:string external;"
					+ " let $p := doc(\"freiburgnet/plans.xml\")/plans return if($p/person[@id=$id]) then insert node "+xml+" into $p/person[@id=$id] else insert node <person id=\"{string($id)}\">"+xml+"</person> into $p";
			try {
				/*XQResultSequence rs = */xqe.executeQuery(query);
			} catch (XQException e) {
				// FORWARD_ONLY_SEQUENCE
				e.printStackTrace();
			}

			/*ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			rs.writeSequence(outStream, new Properties());*/

			conn.close();

			// debug
			/*return Response.ok(outStream.toString(), MediaType.TEXT_PLAIN).build();*/
			return Response.ok().build();
		}
		catch (XQException e) {
			e.printStackTrace();
			return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(e.getMessage()).build();
		}

	}


} 